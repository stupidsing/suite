package suite.concurrent;

import static primal.statics.Fail.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import suite.concurrent.stm.ObstructionFreeStm;
import suite.concurrent.stm.ObstructionFreeStm.Memory;
import suite.concurrent.stm.Stm.Transaction;
import suite.concurrent.stm.Stm.TransactionStatus;

public class ObstructionFreeStmTest {

	private Random random = new Random();

	private int nMemories = 5;
	private int nTransactions = 1;

	private class Worker {
		private ObstructionFreeStm stm;
		private Transaction transaction;
		private List<Integer> orders = new ArrayList<>();
		private List<Integer> adjustments = new ArrayList<>();

		private int step;
		private List<Integer> readValues = new ArrayList<>(Collections.nCopies(nMemories, 0));

		private Worker(ObstructionFreeStm stm) {
			this.stm = stm;
			this.transaction = stm.begin();

			IntStream.range(0, nMemories * 2).forEach(i -> orders.add(i % nMemories));
			Collections.shuffle(orders, random);

			var isRead = new HashSet<>();

			IntStream.range(0, nMemories * 2).forEach(i -> {
				var mi = orders.get(i);
				orders.set(i, isRead.add(mi) ? mi : mi + nMemories);
			});

			orders.add(nMemories * 2);

			var sum = 0;

			for (var i = 1; i < nMemories; i++) {
				var adjustment = random.nextInt(100) - 50;
				adjustments.add(adjustment);
				sum += adjustment;
			}

			adjustments.add(-sum);
		}

		private void work(List<Memory<Integer>> memories) {
			var order = orders.get(step++);

			if (nMemories * 2 <= order) { // commit or rollback
				System.out.println(this + " COMMIT");
				transaction.end(TransactionStatus.DONE____);
			} else if (nMemories <= order) { // write a memory
				var mi = order - nMemories;
				System.out.println(this + " WRITE " + mi);

				var memory = memories.get(mi);
				var read = stm.get(transaction, memory);

				if (read == readValues.get(mi))
					stm.put(transaction, memory, read + adjustments.get(mi));
				else
					fail("value changed between reads");
			} else { // read a memory
				var mi = order;
				System.out.println(this + " READ " + mi);

				var memory = memories.get(mi);
				var read = stm.get(transaction, memory);
				readValues.set(mi, read);
			}
		}
	}

	@BeforeEach
	public void before() {
		random.setSeed(0);
	}

	@Test
	public void test() {
		var stm = new ObstructionFreeStm();
		var memories = IntStream.range(0, nMemories) //
				.mapToObj(i -> stm.newMemory(0)).collect(Collectors.toList());
		var workers = IntStream.range(0, nTransactions) //
				.mapToObj(i -> new Worker(stm)).collect(Collectors.toList());

		var workingOrders = new ArrayList<Integer>();
		IntStream.range(0, nMemories * 2 + 1).forEach(mi -> IntStream.range(0, nTransactions).forEach(workingOrders::add));
		Collections.shuffle(workingOrders, random);

		for (var workingOrder : workingOrders)
			workers.get(workingOrder).work(memories);

		stm.transaction(transaction -> {
			var sum = 0;

			for (var memory : memories) {
				int read = stm.get(transaction, memory);
				System.out.println("FINAL MEMORY VALUE = " + read);
				sum += read;
			}

			if (sum == 0)
				return true;
			else
				return fail("final sum is not zero, but is " + sum);
		});
	}

}

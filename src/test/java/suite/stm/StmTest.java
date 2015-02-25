package suite.stm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;

import suite.stm.Stm.Memory;
import suite.stm.Stm.Transaction;
import suite.stm.Stm.TransactionException;

public class StmTest {

	private Random random = new Random();

	private int nMemories = 5;
	private int nTransactions = 1;

	private class Worker {
		private Transaction transaction;
		private List<Integer> orders = new ArrayList<>();
		private List<Integer> adjustments = new ArrayList<>();

		private int step;
		private List<Integer> readValues = new ArrayList<>(Collections.nCopies(nMemories, 0));

		private Worker(Transaction transaction) {
			this.transaction = transaction;

			IntStream.range(0, nMemories * 2).forEach(i -> orders.add(i % nMemories));
			Collections.shuffle(orders, random);

			Set<Integer> isRead = new HashSet<>();

			IntStream.range(0, nMemories * 2).forEach(i -> {
				Integer mi = orders.get(i);
				orders.set(i, isRead.add(mi) ? mi : mi + nMemories);
			});

			orders.add(nMemories * 2);

			int sum = 0;

			for (int i = 1; i < nMemories; i++) {
				int adjustment = random.nextInt(100) - 50;
				adjustments.add(adjustment);
				sum += adjustment;
			}

			adjustments.add(-sum);
		}

		private void work(List<Memory<Integer>> memories) throws InterruptedException, TransactionException {
			int order = orders.get(step++);

			if (order >= nMemories * 2) { // Commit or rollback
				System.out.println(this + " COMMIT");
				boolean isCommitted = false;
				try {
					transaction.commit();
					isCommitted = true;
				} finally {
					if (!isCommitted)
						transaction.rollback();
				}
			} else if (order >= nMemories) { // Write a memory
				int mi = order - nMemories;
				System.out.println(this + " WRITE " + mi);

				Memory<Integer> memory = memories.get(mi);
				int read = memory.read(transaction);

				if (read == readValues.get(mi))
					memory.write(transaction, read + adjustments.get(mi));
				else
					throw new RuntimeException("Value changed between reads");
			} else { // Read a memory
				int mi = order;
				System.out.println(this + " READ " + mi);

				Memory<Integer> memory = memories.get(mi);
				Integer read = memory.read(transaction);
				readValues.set(mi, read);
			}
		}
	}

	@Before
	public void before() {
		random.setSeed(0);
	}

	@Test
	public void test() throws InterruptedException, TransactionException {
		ObstructionFreeStm stm = new ObstructionFreeStm();
		List<Memory<Integer>> memories = IntStream.range(0, nMemories) //
				.mapToObj(i -> stm.createMemory(Integer.class, 0)).collect(Collectors.toList());
		List<Worker> workers = IntStream.range(0, nTransactions) //
				.mapToObj(i -> new Worker(stm.createTransaction(null))).collect(Collectors.toList());

		List<Integer> workingOrders = new ArrayList<>();
		IntStream.range(0, nMemories * 2 + 1).forEach(mi -> //
				IntStream.range(0, nTransactions).forEach(workingOrders::add));
		Collections.shuffle(workingOrders, random);

		for (int workingOrder : workingOrders)
			workers.get(workingOrder).work(memories);

		Stm.doTransaction(stm, transaction -> {
			int sum = 0;

			for (Memory<Integer> memory : memories) {
				int read = memory.read(transaction);
				System.out.println("FINAL MEMORY VALUE = " + read);
				sum += read;
			}

			if (sum != 0)
				throw new RuntimeException("Final sum is not zero, but is " + sum);
		});
	}

}

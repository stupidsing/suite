package suite.nntp;

import primal.adt.Pair;

import java.util.List;
import java.util.Map;

public interface Nntp {

	public static String contentKey = "::contentKey";

	public List<String> listGroupIds();

	public Pair<String, String> getArticleIdRange(String groupId);

	public List<String> listArticleIds(String groupId, long fromTime);

	public Map<String, String> getArticle(String groupId, String articleId);

	public String createArticle(String groupId, Map<String, String> data);

	public void updateArticle(String articleId, Map<String, String> data);

}

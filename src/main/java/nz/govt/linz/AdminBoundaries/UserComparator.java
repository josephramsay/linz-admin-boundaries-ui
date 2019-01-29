package nz.govt.linz.AdminBoundaries;

import java.util.Comparator;

public class UserComparator implements Comparator <User>{
	@Override
	public int compare(User user1, User user2) {
		return user1.userName.compareTo(user2.userName);
	}
}
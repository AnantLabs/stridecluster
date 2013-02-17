import java.net.InetAddress;

public class QuickSort {
	/*public void sort(int[] array) {
		this.sort(array, 0, array.length - 1);
	}

	public void sort(int[] array, int start, int end) {

	}

	public static void main(String[] args) {
		int[] array = new int[] { 1, 4, 6, 23, 6, 563, 2, 234, 5, 56, 5, 6, 7, 56, 8788, 78, 42, 89 };
		QuickSort qs = new QuickSort();
		qs.sort(array);
		for (int i = 0; i < array.length; i++) {
			System.out.print(array[i] + " , ");
		}
	}*/
	public static void main(String[] args) throws Exception{

		int concurrencyLevel =7;
		/*int ssize = 1;
		while (ssize < concurrencyLevel) {
			ssize <<= 1;
		}

		System.out.println(ssize);*/
		
		String hostname =InetAddress.getLocalHost().getHostName();
		String hostname2 = InetAddress.getLocalHost().getCanonicalHostName();
		
		System.out.println(hostname);
		System.out.println(hostname2);
	}
}

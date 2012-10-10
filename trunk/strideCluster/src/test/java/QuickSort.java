public class QuickSort {
	public void sort(int[] array) {
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
	}
}

package com.example.photoarchive.tools;

import lombok.ToString;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.random.RandomGenerator;

@ToString
public class RingRandomSequence implements Iterator<Integer> {
	private static final RandomGenerator generator = RandomGenerator.getDefault();
	private final int[] numbers;
	private int pointer;

	public RingRandomSequence(int count) {
		assert count > 0;
		numbers = new int[count];
		for (int i = 0; i < count; i++) numbers[i] = i;
		for (int i = 0; i < count; i++) if (numbers[i] == i) shuffle(i);
		pointer = 0;
	}

	public RingRandomSequence(int[] another) {
		assert Objects.nonNull(another);
		numbers = another;
		pointer = 0;
	}

	private void shuffle(int index) {
		if (numbers.length == 1) return;
		if (numbers.length == 2 && generator.nextInt(2) == 0) return;
		var random = index;
		while (random == index) random = generator.nextInt(numbers.length);
		var store = numbers[index];
		numbers[index] = numbers[random];
		numbers[random] = store;
	}

	public static RingRandomSequence sum(RingRandomSequence... another) {
		var newSize = Arrays.stream(another).mapToInt(a -> a.numbers.length).sum();
		var numbers = new int[newSize];
		for (int i = 0, last = 0; i < another.length; last += another[i++].numbers.length) {
			for (int j = 0; j < another[i].numbers.length; j++) {
				numbers[last + j] = another[i].numbers[j] + last;
			}
		}
		return new RingRandomSequence(numbers);
	}

	private int changeBy(int change) {
		if (numbers.length == 0) return 0;
		var result = pointer + change;
		while (result >= numbers.length) result -= numbers.length;
		while (result < 0) result += numbers.length;
		return result;
	}

	public int suppose(int offset) {
		return numbers[changeBy(offset)];
	}

//	public void move(int offset) {
//		pointer = changeBy(offset);
//	}

	public int current() {
		return numbers[pointer];
	}

	public int previous() {
		pointer = changeBy(-1);
		return numbers[pointer];
	}

	@Override
	public boolean hasNext() {
		return true;
	}

	@Override
	public Integer next() {
		pointer = changeBy(1);
		return numbers[pointer];
	}

	public int size() {
		return numbers.length;
	}
}

package com.syntm.util;
/*
Author:  Yehia Abd Alrahman (yehiaa@chalmers.se)
StringUtil.java (c) 2024
Desc: String utility class
Created:  17/11/2024 09:45:55
Updated:  17/11/2024 12:45:02
Version:  1.1
*/


import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

public class StringUtil {
	public static String join(final Iterable<? extends Object> elements, final CharSequence separator) {
		StringBuilder builder = new StringBuilder();

		if (elements != null) {
			Iterator<? extends Object> iter = elements.iterator();
			if (iter.hasNext()) {
				builder.append(String.valueOf(iter.next()));
				while (iter.hasNext()) {
					builder
							.append(separator)
							.append(String.valueOf(iter.next()));
				}
			}
		}

		return builder.toString();
	}

	public static Iterable<String> wrapEach(final Iterable<String> elements, final String formatString) {
		Collection<String> out = new LinkedList<String>();

		for (String s : elements) {
			out.add(String.format(formatString, s));
		}

		return out;
	}

	public static Iterable<String> removePrefixEach(final Iterable<String> elements) {
		return StringUtil.removePrefixEach(elements, 1);
	}

	public static Iterable<String> removePrefixEach(final Iterable<String> elements, final int amount) {
		Collection<String> out = new LinkedList<String>();

		for (String s : elements) {
			out.add(s.substring(1));
		}

		return out;
	}
}

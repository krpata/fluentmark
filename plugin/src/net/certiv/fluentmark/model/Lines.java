/*******************************************************************************
 * Copyright (c) 2016 - 2017 Certiv Analytics and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package net.certiv.fluentmark.model;

import java.util.LinkedHashMap;
import java.util.Map;

import net.certiv.fluentmark.FluentMkUI;
import net.certiv.fluentmark.preferences.Prefs;
import net.certiv.fluentmark.util.FloorKeyMap;
import net.certiv.fluentmark.util.Indent;

public class Lines {

	protected static class Line {

		String text = "";
		int offset = 0;
		int length = 0;
		int idx = -1;
		Kind kind = Kind.UNDEFINED;	// effective
		Kind nKind = kind; 			// natural/original
		PagePart part;

		private boolean blankPrior;
		private boolean blankNext;

		public Line() {
			blankPrior = true;
			blankNext = true;
		}

		public Line(int delimLen, Line prior, String line) {
			this();
			this.text = line;
			this.offset = prior.offset + prior.length;
			this.length = line.length() + delimLen;
			this.idx = prior.idx + 1;

			blankPrior = prior.isBlank();
			prior.blankNext = isBlank();
		}

		public String toString() {
			return String.format("%4d %-6.6s/%-6.6s [%5d:%3d] %s", //
					idx, kind.toString(), nKind.toString(), offset, length, text);
		}

		/** Just ws. */
		public boolean isBlank() {
			return text.trim().isEmpty();
		}

		public boolean isBlankPrior() {
			return blankPrior;
		}

		public boolean isBlankNext() {
			return blankNext;
		}
	}

	private Map<Integer, Line> lineList = new LinkedHashMap<>();
	private FloorKeyMap lineMap;
	private String lineDelim;

	public Lines(String content, String lineDelim) {
		this.lineDelim = lineDelim;
		load(content);
	}

	private void load(String content) {
		String[] lines = content.split(lineDelim, -1); // do not skip blank lines
		int delimLen = lineDelim != null ? lineDelim.length() : 0;
		if (lineMap != null) lineMap.clear();
		lineMap = new FloorKeyMap(lines.length);
		Line prior = new Line();
		for (int idx = 0; idx < lines.length; idx++) {
			Line line = new Line(delimLen, prior, lines[idx]);
			lineList.put(idx, line);
			lineMap.put(line.offset, idx);
			prior = line;
		}
	}

	/** Number of lines in content */
	public int length() {
		return lineList.size();
	}

	public FloorKeyMap getOffsetMap() {
		return lineMap;
	}

	public Line getLine(int idx) {
		return lineList.get(idx);
	}

	public int getOffset(int idx) {
		return lineList.get(idx).offset;
	}

	public String getText(int idx) {
		return lineList.get(idx).text;
	}

	public int getTextLength(int idx) {
		return lineList.get(idx).length;
	}

	public Kind getKind(int idx) {
		return lineList.get(idx).kind;
	}

	public Kind getOriginalKind(int idx) {
		return lineList.get(idx).nKind;
	}

	public void setKind(int idx, Kind kind) {
		Line line = lineList.get(idx);
		line.kind = kind;
		if (line.nKind == Kind.UNDEFINED) {
			line.nKind = kind; // preserve the original kind
		}
	}

	public PagePart getPagePart(int idx) {
		return lineList.get(idx).part;
	}

	public void setPagePart(int idx, PagePart part) {
		lineList.get(idx).part = part;
	}

	public Kind identifyKind(int idx) {
		Line curr = lineList.get(idx);
		Line next = lineList.size() > idx + 1 ? lineList.get(idx + 1) : null;

		String curTxt = curr.text;
		String nxtTxt = next != null ? next.text : "";

		boolean priorblank = curr.isBlankPrior();
		boolean curblank = curr.isBlank();
		boolean nxtblank = next != null ? next.isBlank() : true;
		boolean nxtnxtblank = next != null ? next.isBlankNext() : true;

		if (curblank) return Kind.BLANK;

		if (idx == 0 && curTxt.startsWith("---")) return Kind.FRONT_MATTER;

		if (curTxt.startsWith("#")) return Kind.HEADER;
		if (!curblank && nxtnxtblank && (nxtTxt.startsWith("---") || nxtTxt.startsWith("==="))) return Kind.HEADER;
		if (!priorblank && nxtblank && (curTxt.startsWith("---") || curTxt.startsWith("==="))) return Kind.SETEXT;

		if (curTxt.startsWith("$$")) return Kind.MATH_BLOCK;
		if (priorblank && nxtblank && curTxt.matches("\\$\\S.*?\\S\\$.*")) return Kind.MATH_BLOCK_INLINE;

		if (curTxt.startsWith("<!---")) return Kind.COMMENT;
		if (curTxt.startsWith("--->")) return Kind.COMMENT;

		if (priorblank && nxtblank && curTxt.startsWith("___")) return Kind.HRULE;
		if (priorblank && nxtblank && curTxt.startsWith("***")) return Kind.HRULE;
		if (priorblank && nxtblank && curTxt.startsWith("---")) return Kind.HRULE;

		if (curTxt.matches("(\\|\\s?\\:?---+\\:?\\s?)+\\|.*")) return Kind.TABLE;

		if (curTxt.matches("\\s*\\*\\s+.*")) return Kind.LIST;
		if (curTxt.matches("\\s*\\-\\s+.*")) return Kind.LIST;
		if (curTxt.matches("\\s*\\+\\s+.*")) return Kind.LIST;
		if (curTxt.matches("\\s*\\d+\\.\\s+.*")) return Kind.LIST;

		if (curTxt.matches("(\\>+\\s+)+.*")) return Kind.QUOTE;
		if (curTxt.matches("\\:\\s+.*")) return Kind.DEFINITION;
		if (curTxt.matches("\\[\\^?\\d+\\]\\:\\s+.*")) return Kind.REFERENCE;

		if (curTxt.matches("\\</?\\w+(\\s+.*?)?/?\\>.*")) return Kind.HTML_BLOCK;

		if (curTxt.startsWith("```")) return Kind.CODE_BLOCK;
		if (curTxt.startsWith("~~~")) return Kind.CODE_BLOCK;
		if (curTxt.matches("    .*")) return Kind.CODE_BLOCK_INDENTED;

		return Kind.TEXT;
	}

	public int nextMatching(int mark, String exact) {
		return nextMatching(mark, null, exact);
	}

	public int nextMatching(int mark, Kind kind) {
		return nextMatching(mark, kind, null);
	}

	public int nextMatching(int mark, Kind kind, String exact) {
		for (int idx = mark + 1; idx < length(); idx++) {
			boolean ok = kind != null ? kind == identifyKind(idx) : true;
			ok = ok && (exact != null) ? lineList.get(idx).text.startsWith(exact) : ok;
			if (ok) return idx;
		}
		return length() - 1;
	}

	public void clear() {
		lineList.clear();
	}

	public void dispose() {
		clear();
	}

	public static int computeLevel(String text) {
		int width = FluentMkUI.getDefault().getPreferenceStore().getInt(Prefs.EDITOR_TAB_WIDTH);
		return Indent.measureIndentInSpaces(text, width);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Line line : lineList.values()) {
			sb.append(line.toString());
		}
		return sb.toString();
	}
}

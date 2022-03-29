/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.source.formatter.check;

import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.io.unsync.UnsyncBufferedReader;
import com.liferay.portal.kernel.io.unsync.UnsyncStringReader;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.io.IOException;

/**
 * @author Hugo Huijser
 */
public class PropertiesEmptyLinesCheck extends BaseFileCheck {

	@Override
	protected String doProcess(
			String fileName, String absolutePath, String content)
		throws IOException {

		if (!fileName.endsWith(".properties")) {
			return content;
		}

		return _fixMissingEmptyLines(content);
	}

	private String _fixMissingEmptyLines(String content) throws IOException {
		int lineNumber = 0;

		StringBundler sb = new StringBundler();

		String previousLine = StringPool.BLANK;

		try (UnsyncBufferedReader unsyncBufferedReader =
				new UnsyncBufferedReader(new UnsyncStringReader(content))) {

			String line = null;

			while ((line = unsyncBufferedReader.readLine()) != null) {
				lineNumber++;

				if (Validator.isNull(line)) {
					sb.append(StringPool.NEW_LINE);
					previousLine = StringPool.BLANK;

					continue;
				}

				String trimLine = StringUtil.trim(line);

				if (StringUtil.equals(trimLine, StringPool.BACK_SLASH) ||
					StringUtil.equals(trimLine, "#\\")) {

					sb.append(line);
					sb.append(StringPool.NEW_LINE);
					previousLine = trimLine;

					continue;
				}

				if (!trimLine.startsWith("#")) {
					int index = trimLine.indexOf("#");

					if ((index != -1) && (index < (trimLine.length() - 1)) &&
						(trimLine.charAt(index + 1) == CharPool.SPACE)) {

						trimLine = StringUtil.trim(
							trimLine.substring(0, index));
					}
				}

				if (!trimLine.startsWith("#") &&
					(StringUtil.equals(previousLine, "##") ||
					 StringUtil.equals(previousLine, "#"))) {

					sb.append(StringPool.NEW_LINE);
					sb.append(line);
					sb.append(StringPool.NEW_LINE);
					previousLine = trimLine;

					continue;
				}

				if (trimLine.startsWith("##") &&
					Validator.isNotNull(previousLine) &&
					!previousLine.startsWith("##")) {

					sb.append(StringPool.NEW_LINE);
					sb.append(line);
					sb.append(StringPool.NEW_LINE);
					previousLine = trimLine;

					continue;
				}

				if (trimLine.matches("[^#]+=\\\\") &&
					Validator.isNotNull(previousLine)) {

					sb.append(StringPool.NEW_LINE);
					sb.append(line);
					sb.append(StringPool.NEW_LINE);
					previousLine = trimLine;

					continue;
				}

				String nextLine = getLine(content, lineNumber + 1);

				if (Validator.isNotNull(nextLine)) {
					nextLine = StringUtil.trim(nextLine);
				}
				else {
					nextLine = StringPool.BLANK;
				}

				if (!trimLine.startsWith("#") &&
					!trimLine.endsWith(StringPool.BACK_SLASH) &&
					previousLine.endsWith(StringPool.BACK_SLASH) &&
					Validator.isNotNull(nextLine) &&
					!nextLine.matches("^#(?![ #]).+")) {

					sb.append(line);
					sb.append(StringPool.NEW_LINE);
					sb.append(StringPool.NEW_LINE);
					previousLine = StringPool.BLANK;

					continue;
				}

				if (trimLine.startsWith("#") &&
					trimLine.matches(_MULTI_COMMENTS_REGEX)) {

					if (StringUtil.equals(previousLine, "\\") ||
						StringUtil.equals(previousLine, "#\\") ||
						previousLine.matches("[^#]+=\\\\*")) {

						sb.append(line);
						sb.append(StringPool.NEW_LINE);
						previousLine = trimLine;

						continue;
					}

					if (Validator.isNotNull(previousLine) &&
						!previousLine.matches("^((# +.+)|#)$") &&
						nextLine.matches(_MULTI_COMMENTS_REGEX)) {

						sb.append(StringPool.NEW_LINE);
						sb.append(line);
						sb.append(StringPool.NEW_LINE);
						previousLine = trimLine;

						continue;
					}
				}

				if (trimLine.matches("^#(?![ #]).+") &&
					previousLine.matches(_MULTI_COMMENTS_REGEX) &&
					Validator.isNotNull(nextLine)) {

					String statement = trimLine.replaceAll(
						StringPool.DOUBLE_BACK_SLASH, StringPool.BLANK);
					String nextStatement = nextLine.replaceAll(
						StringPool.DOUBLE_BACK_SLASH, StringPool.BLANK);
					statement = statement.substring(1);

					if (nextLine.startsWith("#") && (nextLine.length() > 1)) {
						nextStatement = nextStatement.substring(1);
					}

					if (nextLine.endsWith("\\")) {
						if (StringUtil.equals(statement, nextStatement)) {
							sb.append(line);
							sb.append(StringPool.NEW_LINE);
							sb.append(StringPool.NEW_LINE);
							previousLine = StringPool.BLANK;
						}
						else {
							sb.append(StringPool.NEW_LINE);
							sb.append(line);
							sb.append(StringPool.NEW_LINE);
							previousLine = trimLine;
						}
					}
					else {
						String[] statementArray = statement.split(
							StringPool.EQUAL);
						String[] nextStatementArray = nextStatement.split(
							StringPool.EQUAL);

						if ((statementArray.length == 1) &&
							(nextStatementArray.length == 2) &&
							StringUtil.equals(
								statementArray[0], nextStatementArray[0])) {

							sb.append(line);
							sb.append(StringPool.NEW_LINE);
							sb.append(StringPool.NEW_LINE);
							previousLine = StringPool.BLANK;
						}
						else {
							sb.append(StringPool.NEW_LINE);
							sb.append(line);
							sb.append(StringPool.NEW_LINE);
							previousLine = trimLine;
						}
					}

					continue;
				}

				sb.append(line);
				sb.append(StringPool.NEW_LINE);
				previousLine = trimLine;
			}
		}

		if (sb.index() > 0) {
			sb.setIndex(sb.index() - 1);
		}

		return sb.toString();
	}

	private static final String _MULTI_COMMENTS_REGEX = "^((# (?! ).+)|#)$";

}
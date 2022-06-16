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

package com.liferay.source.formatter.checkstyle.check;

import com.liferay.portal.kernel.util.StringUtil;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FullIdent;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

import java.util.List;

/**
 * @author Qi Zhang
 */
public class StringBundlerUseCheck extends BaseCheck {

	@Override
	public int[] getDefaultTokens() {
		return new int[] {TokenTypes.VARIABLE_DEF};
	}

	@Override
	protected void doVisitToken(DetailAST detailAST) {
		String typeName = getTypeName(detailAST, false);

		if (!typeName.equals("StringBundler")) {
			return;
		}

		DetailAST modifiersDetailAST = detailAST.findFirstToken(
			TokenTypes.MODIFIERS);

		if (modifiersDetailAST.branchContains(TokenTypes.LITERAL_PROTECTED) ||
			modifiersDetailAST.branchContains(TokenTypes.LITERAL_PUBLIC)) {

			return;
		}

		String name = getName(detailAST);

		DetailAST parentDetailAST = detailAST.getParent();

		if ((parentDetailAST == null) ||
			(parentDetailAST.getType() != TokenTypes.SLIST)) {

			return;
		}

		List<DetailAST> detailASTS = getVariableCallerDetailASTList(
			detailAST, name);

		boolean alreadyToString = false;

		int appendCount = 0;

		for (DetailAST tmpDetailAST : detailASTS) {
			DetailAST tmpParentDetailAST = tmpDetailAST.getParent();

			if (tmpParentDetailAST.getType() == TokenTypes.DOT) {
				FullIdent fullIdent = FullIdent.createFullIdent(
					tmpParentDetailAST);

				if (StringUtil.equals(name + ".append", fullIdent.getText())) {
					if (alreadyToString) {
						return;
					}

					DetailAST sListParentDetailAST = getParentWithTokenType(
						detailAST, TokenTypes.SLIST);

					if (sListParentDetailAST.getLineNo() !=
							parentDetailAST.getLineNo()) {

						return;
					}

					appendCount++;
				}
				else if (StringUtil.equals(
							name + ".toString", fullIdent.getText())) {

					alreadyToString = true;
				}
			}
			else {
				break;
			}
		}

		if (appendCount > 3) {
			log(detailAST, _MSG_USE_STRING_CONCAT);
		}
		else {
			log(detailAST, _MSG_USE_STRING_ADD);
		}
	}

	private static final String _MSG_USE_STRING_ADD = "use.string.add";

	private static final String _MSG_USE_STRING_CONCAT = "use.string.concat";

}
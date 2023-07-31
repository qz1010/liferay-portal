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

import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.StringUtil;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

import java.util.List;

/**
 * @author Qi Zhang
 */
public class BooleanReturnCheck extends BaseCheck {

	@Override
	public int[] getDefaultTokens() {
		return new int[] {
			TokenTypes.METHOD_DEF
		};
	}

	@Override
	protected void doVisitToken(DetailAST detailAST) {


		DetailAST typeDetailAST = detailAST.findFirstToken(TokenTypes.TYPE);

		if (typeDetailAST != null) {
			String methodTypename = getTypeName(typeDetailAST, true);

			if (StringUtil.equals(StringUtil.toLowerCase(methodTypename), "boolean")) {
//				SFDebugHelper.printStructure(detailAST);
				List<DetailAST> childDetailASTs = getAllChildTokens(detailAST, true, TokenTypes.LITERAL_RETURN);

				if (ListUtil.isEmpty(childDetailASTs) || childDetailASTs.size() <= 1) {
					return;
				}

				DetailAST lastDetailAST = childDetailASTs.get(childDetailASTs.size() - 1);
				if (_checkReturnTypeIsBoolean(lastDetailAST)) {
					return;
				}

				DetailAST nextDetailAST = lastDetailAST.getNextSibling();
//				SFDebugHelper.printStructure(nextDetailAST);
				if (nextDetailAST == null || nextDetailAST.getType() != TokenTypes.RCURLY) {
					return;
				}

				DetailAST preDetailAST = lastDetailAST.getPreviousSibling();

				if (_checkPreDetailAST(preDetailAST)) {
					return;
				}

				log(preDetailAST, _MSG_RETURN_LOGIC_SIMPLY);
			}
		}
	}

	private boolean _checkReturnTypeIsBoolean(DetailAST detailAST) {
		DetailAST exprDetailAST = detailAST.getFirstChild();

		if (exprDetailAST == null) {
			return true;
		}

		DetailAST typeDetailAST = exprDetailAST.getFirstChild();

		return typeDetailAST.getType() != TokenTypes.LITERAL_FALSE &&
				typeDetailAST.getType() != TokenTypes.LITERAL_TRUE;
	}

	private boolean _checkPreDetailAST(DetailAST detailAST) {
		if (detailAST == null || detailAST.getType() != TokenTypes.LITERAL_IF) {
			return true;
		}

		DetailAST exprDetailAST = detailAST.findFirstToken(TokenTypes.EXPR);

		DetailAST exprFirstChildDetailAST = exprDetailAST.getFirstChild();

		int type = exprFirstChildDetailAST.getType();

		if (type == TokenTypes.BOR || type == TokenTypes.LOR ||
				type == TokenTypes.BAND || type == TokenTypes.LAND) {
			return true;
		}

		DetailAST sListDetailAST = detailAST.findFirstToken(TokenTypes.SLIST);

		if (sListDetailAST == null) {
			return true;
		}

		DetailAST firstChildDetailAST = sListDetailAST.getFirstChild();

		if (firstChildDetailAST == null || firstChildDetailAST.getType() != TokenTypes.LITERAL_RETURN) {
			return true;
		}

		return _checkReturnTypeIsBoolean(firstChildDetailAST);
	}

	private static final String _MSG_RETURN_LOGIC_SIMPLY = "return.logic.simply";

}
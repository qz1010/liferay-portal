/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.source.formatter.checkstyle.check;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

import java.util.List;

/**
 * @author Hugo Huijser
 */
public class AssignAsUsedCheck extends BaseAsUsedCheck {

	@Override
	public int[] getDefaultTokens() {
		return new int[] {TokenTypes.CTOR_DEF, TokenTypes.METHOD_DEF};
	}

	@Override
	protected void doVisitToken(DetailAST detailAST) {
		List<DetailAST> assignDetailASTList = getAllChildTokens(
			detailAST, true, TokenTypes.ASSIGN);

		for (DetailAST assignDetailAST : assignDetailASTList) {
			DetailAST parentDetailAST = assignDetailAST.getParent();

			if (parentDetailAST.getType() != TokenTypes.EXPR) {
				continue;
			}

			parentDetailAST = parentDetailAST.getParent();

			if (parentDetailAST.getType() == TokenTypes.SLIST) {
				_checkAssign(
					detailAST, assignDetailAST,
					getEndLineNumber(parentDetailAST));
			}
		}
	}

	private void _checkMoveInsideLambdaStatement(
			DetailAST assignDetailAST, DetailAST nameDetailAST, String variableName,
			DetailAST firstDependentIdentDetailAST,
			DetailAST variableDefinitionDetailAST, int actionLineNumber) {

		DetailAST lambdaStatementDetailAST = _getLambdaStatementDetailAST(
				firstDependentIdentDetailAST, getEndLineNumber(assignDetailAST));

		if (lambdaStatementDetailAST == null) {
			return;
		}

		DetailAST parentDetailAST = getParentWithTokenType(
				lambdaStatementDetailAST, TokenTypes.METHOD_CALL);

		if (parentDetailAST == null ||
				(parentDetailAST.getLineNo() < assignDetailAST.getLineNo())) {
			return;
		}

		parentDetailAST = getParentWithTokenType(
				parentDetailAST, TokenTypes.LAMBDA,
				TokenTypes.LITERAL_DO, TokenTypes.LITERAL_FOR,
				TokenTypes.LITERAL_SYNCHRONIZED,
				TokenTypes.LITERAL_TRY, TokenTypes.LITERAL_WHILE);

		if ((parentDetailAST != null) &&
				(parentDetailAST.getLineNo() >= assignDetailAST.getLineNo())) {

			return;
		}

		DetailAST slistDetailAST = lambdaStatementDetailAST.findFirstToken(
				TokenTypes.SLIST);

		List<DetailAST> dependentIdentDetailASTs =
				getDependentIdentDetailASTList(variableDefinitionDetailAST,
						variableDefinitionDetailAST.getLineNo(),
						true);

		if (getEndLineNumber(slistDetailAST) <=
				dependentIdentDetailASTs.get(
						dependentIdentDetailASTs.size() - 1).getLineNo()) {

			return;
		}

		if (actionLineNumber != -1) {
			if (actionLineNumber < lambdaStatementDetailAST.getLineNo()) {
				return;
			}
		}

		log(nameDetailAST, _MSG_MOVE_VARIABLE_INSIDE_IF_STATEMENT,
				variableName, "lambda", lambdaStatementDetailAST.getLineNo());

	}

	private DetailAST _getLambdaStatementDetailAST(
			DetailAST detailAST, int lineNumber) {

		DetailAST lambdaStatementDetailAST = null;

		DetailAST slistDetailAST = getParentWithTokenType(
				detailAST, TokenTypes.SLIST);

		while (true) {
			if ((slistDetailAST == null) ||
					(slistDetailAST.getLineNo() < lineNumber)) {

				return lambdaStatementDetailAST;
			}

			DetailAST parentDetailAST = slistDetailAST.getParent();

			if ((parentDetailAST.getType() == TokenTypes.LAMBDA)) {

				lambdaStatementDetailAST = parentDetailAST;
			}

			slistDetailAST = getParentWithTokenType(
					slistDetailAST, TokenTypes.SLIST);
		}
	}

	private void _checkAssign(
		DetailAST detailAST, DetailAST assignDetailAST, int endRange) {

		if (hasParentWithTokenType(
				assignDetailAST, TokenTypes.FOR_EACH_CLAUSE,
				TokenTypes.FOR_INIT)) {

			return;
		}

		DetailAST nameDetailAST = assignDetailAST.getFirstChild();

		if (nameDetailAST.getType() != TokenTypes.IDENT) {
			return;
		}

		String variableName = nameDetailAST.getText();

		DetailAST typeDetailAST = getVariableTypeDetailAST(
			assignDetailAST, variableName, false);

		if (typeDetailAST == null) {
			return;
		}

		DetailAST parentDetailAST = typeDetailAST.getParent();

		if (parentDetailAST.getType() != TokenTypes.VARIABLE_DEF) {
			return;
		}

		List<DetailAST> dependentIdentDetailASTList =
			getDependentIdentDetailASTList(
				parentDetailAST, parentDetailAST.getLineNo());

		if (dependentIdentDetailASTList.isEmpty()) {
			return;
		}

		int endLineNumber = getEndLineNumber(assignDetailAST);

		for (DetailAST dependentIdentDetailAST : dependentIdentDetailASTList) {
			int lineNumber = dependentIdentDetailAST.getLineNo();

			if (lineNumber <= endLineNumber) {
				continue;
			}

			if (lineNumber > endRange) {
				return;
			}

			if (!hasParentWithTokenType(
					assignDetailAST, TokenTypes.LITERAL_FOR,
					TokenTypes.LITERAL_WHILE)) {

				int actionLineNumber = getActionLineNumber(assignDetailAST);

				if (actionLineNumber != assignDetailAST.getLineNo()) {
					checkMoveAfterBranchingStatement(
						detailAST, assignDetailAST, variableName,
						dependentIdentDetailAST, actionLineNumber);
					checkMoveInsideIfStatement(
						assignDetailAST, nameDetailAST, variableName,
						dependentIdentDetailAST,
						dependentIdentDetailASTList.get(
							dependentIdentDetailASTList.size() - 1),
						actionLineNumber);
					_checkMoveInsideLambdaStatement(assignDetailAST, nameDetailAST, variableName,dependentIdentDetailAST , parentDetailAST, actionLineNumber);
				}
			}

			parentDetailAST = getParentWithTokenType(
				assignDetailAST, TokenTypes.LITERAL_FOR,
				TokenTypes.LITERAL_WHILE);

			if (parentDetailAST != null) {
				List<String> names = getNames(parentDetailAST, true);

				if (!names.contains(variableName)) {
					checkInline(
						assignDetailAST, variableName, dependentIdentDetailAST,
						dependentIdentDetailASTList);
				}
			}

			return;
		}
	}

	private static final String _MSG_MOVE_VARIABLE_INSIDE_IF_STATEMENT =
			"variable.move.inside.if.statement";

}
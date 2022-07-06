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

import com.liferay.debug.SFDebugHelper;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Qi Zhang
 */
public class TransformUtilCheck extends BaseCheck {

    @Override
    public int[] getDefaultTokens() {
        return new int[]{TokenTypes.LITERAL_FOR};
    }

    @Override
    protected void doVisitToken(DetailAST detailAST) {

//        String typeName = getTypeName(detailAST, false);
//
//        if (!StringUtil.equals("List", typeName)) {
//            return;
//        }
//
//        List<DetailAST> callerDetailASTs = getVariableCallerDetailASTList(detailAST);
//
//        if (callerDetailASTs.size() > 1) {
//            return;
//        }
//
//        DetailAST nextDetailAST = detailAST.getNextSibling();

//        while (nextDetailAST != null) {

            if (detailAST.getType() == TokenTypes.LITERAL_FOR) {

//                    SFDebugHelper.printStructure(detailAST);
                DetailAST foEachClauseDetailAST = detailAST.findFirstToken(TokenTypes.FOR_EACH_CLAUSE);

                if (foEachClauseDetailAST == null) {
                    return;
                }

//                DetailAST childDetailAST = foEachClauseDetailAST.getFirstChild();
//
//                while (childDetailAST != null) {
//
//                    int tokenTypeInForParam = childDetailAST.getType();
//
//                    if (tokenTypeInForParam == TokenTypes.EXPR) {
//                        if (!equals(callerDetailASTs.get(0), childDetailAST.getFirstChild())) {
//                            return;
//                        }
//                    }
//
//                    childDetailAST = childDetailAST.getNextSibling();
//                }

                DetailAST sListDetailAST = detailAST.findFirstToken(TokenTypes.SLIST);

                _checkForBody(sListDetailAST);
            }

//            nextDetailAST = nextDetailAST.getNextSibling();
//        }
    }

    private void _checkForBody(DetailAST detailAST) {

        DetailAST childDetailAST = detailAST.getFirstChild();

        int count = 0;
        while (childDetailAST != null) {

            if (childDetailAST.getType() != TokenTypes.SEMI &&
                    childDetailAST.getType() != TokenTypes.RCURLY) {
                count++;
            }

            childDetailAST = childDetailAST.getNextSibling();
        }

        if (count == 0 || count > 2) {
            return;
        }

        DetailAST lastChildDetailAST = detailAST.getLastChild();

        while (true) {

            if (lastChildDetailAST == null) {
                return;
            }

            if (lastChildDetailAST.getType() == TokenTypes.SEMI ||
                    lastChildDetailAST.getType() == TokenTypes.RCURLY) {
                lastChildDetailAST = lastChildDetailAST.getPreviousSibling();
            } else {
                break;
            }
        }

        int tokenType = lastChildDetailAST.getType();

        if (tokenType != TokenTypes.EXPR) {
            return;
        }

        DetailAST exprChildDetailAST = lastChildDetailAST.getFirstChild();

        if (exprChildDetailAST.getType() != TokenTypes.METHOD_CALL) {
            return;
        }

        DetailAST dotDetailAST = exprChildDetailAST.findFirstToken(
                TokenTypes.DOT);

        if (dotDetailAST == null) {
            return;
        }

        DetailAST dotChildDetailAST = dotDetailAST.getFirstChild();

        if (dotChildDetailAST.getType() != TokenTypes.IDENT) {
            return;
        }

        String variableName = dotDetailAST.getFirstChild().getText();
        String methodName = dotDetailAST.getLastChild().getText();

        if (!StringUtil.equals(methodName, "add")) {
            return;
        }

        DetailAST elistDetailAST = exprChildDetailAST.findFirstToken(TokenTypes.ELIST);

        if (elistDetailAST.getChildCount() > 1) {
            return;
        }

        DetailAST firstChildDetailAST = detailAST.getFirstChild();

        if (!equals(firstChildDetailAST, lastChildDetailAST)) {
            if (firstChildDetailAST.getType() != TokenTypes.VARIABLE_DEF) {
                return;
            }

            String variableNameInBody = getName(firstChildDetailAST);

            if (!_containsVariableName(lastChildDetailAST, variableNameInBody)) {
                return;
            }
        }

        _checkIsUsed(detailAST, variableName);
    }

    private void _checkIsUsed(DetailAST detailAST, String variableName) {

        if (Validator.isNull(variableName)) {
            return;
        }

        DetailAST parentDetailAST = detailAST.getParent();

        DetailAST preDetailAST = parentDetailAST.getPreviousSibling();

        while (preDetailAST != null) {

            int tokenType = preDetailAST.getType();

            if (tokenType == TokenTypes.VARIABLE_DEF) {
                if (StringUtil.equals(getName(preDetailAST), variableName) &&
                        StringUtil.equals(getTypeName(preDetailAST, false), "List")) {

                    log(preDetailAST.getLineNo(), _MSG_USE_TRANSFORM_UTIL, variableName);

                    return;
                }
            } else {
                List<DetailAST> identDetailASTList = getAllChildTokens(
                        preDetailAST, true, TokenTypes.IDENT);

                for (DetailAST identDetailAST : identDetailASTList) {
                    if (!isMethodNameDetailAST(identDetailAST) &&
                            variableName.equals(identDetailAST.getText())) {

                        return;
                    }
                }
            }

            preDetailAST = preDetailAST.getPreviousSibling();
        }
    }

    private boolean _containsVariableName(
            DetailAST detailAST, String variableName) {

        if (variableName == null) {
            return false;
        }

        List<DetailAST> identDetailASTList = getAllChildTokens(
                detailAST, true, TokenTypes.IDENT);

        for (DetailAST identDetailAST : identDetailASTList) {
            if (!isMethodNameDetailAST(identDetailAST) &&
                    variableName.equals(identDetailAST.getText())) {

                return true;
            }
        }

        return false;
    }

    private static final String _MSG_USE_TRANSFORM_UTIL =
            "transform.util.use";
}
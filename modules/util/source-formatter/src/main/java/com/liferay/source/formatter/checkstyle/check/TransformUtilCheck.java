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
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FullIdent;
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
        return new int[]{TokenTypes.VARIABLE_DEF};
    }

    @Override
    protected void doVisitToken(DetailAST detailAST) {

        String typeName = getTypeName(detailAST, false);

        if (!StringUtil.equals("List", typeName)) {
            return;
        }

        String listName = getName(detailAST);

        List<DetailAST> callerDetailASTs = getVariableCallerDetailASTList(detailAST);

        if (callerDetailASTs.size() > 1) {
            return;
        }

        SFDebugHelper.printStructure(detailAST);

        DetailAST nextDetailAST = detailAST.getNextSibling();

        while (nextDetailAST != null) {

            int tokenType = nextDetailAST.getType();

            if (tokenType == TokenTypes.SEMI) {
                nextDetailAST = nextDetailAST.getNextSibling();
            } else if (tokenType == TokenTypes.LITERAL_FOR) {
                DetailAST foEachClauseDetailAST = nextDetailAST.findFirstToken(TokenTypes.FOR_EACH_CLAUSE);

                DetailAST childDetailAST = foEachClauseDetailAST.getFirstChild();

                String paramName = null;

                while (childDetailAST != null) {

                    int tokenTypeInForParam = childDetailAST.getType();
                    if (tokenTypeInForParam == TokenTypes.VARIABLE_DEF) {
                        paramName = getName(childDetailAST);
                    } else if (tokenTypeInForParam == TokenTypes.EXPR) {
                        if (!equals(callerDetailASTs.get(0), childDetailAST.getFirstChild())) {
                            return;
                        }
                    }

                    childDetailAST = childDetailAST.getNextSibling();
                }

                DetailAST sListDetailAST = nextDetailAST.findFirstToken(TokenTypes.SLIST);

                DetailAST childInBodyDetailAST = sListDetailAST.getFirstChild();

                while (true) {

                    int childTokenTypeInBody = childInBodyDetailAST.getType();

                    if (childTokenTypeInBody == TokenTypes.EXPR) {
                        DetailAST exprChildDetailAST =
								childInBodyDetailAST.getFirstChild();

                        if (exprChildDetailAST.getType() == TokenTypes.METHOD_CALL) {
                            DetailAST dotDetailAST = exprChildDetailAST.findFirstToken(
                                    TokenTypes.DOT);

                            if (dotDetailAST == null) {
                                break;
                            }

                            DetailAST dotChildDetailAST = dotDetailAST.getFirstChild();

                            if (dotChildDetailAST.getType() != TokenTypes.IDENT) {
								break;
                            }

							String variableName = dotChildDetailAST.getFirstChild().getText();
							String methodName = dotChildDetailAST.getLastChild().getText();

							if (!StringUtil.equals(methodName, "add")) {
								return;
							}

                            DetailAST elistDetailAST = exprChildDetailAST.findFirstToken(TokenTypes.ELIST);

                            if (elistDetailAST.getChildCount() > 1) {
                                return;
                            }

                        }
                    }

					childInBodyDetailAST = childInBodyDetailAST.getNextSibling();
                }

            }

        }


    }

    private static final String _MSG_UNNEEDED_ARRAY = "array.unneeded";

    private static final String _MSG_USE_LIST_UTIL_FROM_ARRAY =
            "list.util.from.array.use";

    private static final String _MSG_USE_LIST_UTIL_IS_EMPTY =
            "list.util.is.empty.use";

    private static final Log _log = LogFactoryUtil.getLog(ListUtilCheck.class);

    private final Map<String, String> _buildGradleContentsMap =
            new ConcurrentHashMap<>();

}
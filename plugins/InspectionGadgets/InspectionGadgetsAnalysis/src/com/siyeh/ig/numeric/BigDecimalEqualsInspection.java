/*
 * Copyright 2003-2018 Dave Griffith, Bas Leijdekkers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.siyeh.ig.numeric;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.dataFlow.Nullness;
import com.intellij.codeInspection.dataFlow.NullnessUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.util.PsiTreeUtil;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import com.siyeh.ig.PsiReplacementUtil;
import com.siyeh.ig.psiutils.CommentTracker;
import com.siyeh.ig.psiutils.EqualityCheck;
import com.siyeh.ig.psiutils.ExpressionUtils;
import com.siyeh.ig.psiutils.ParenthesesUtils;
import org.jetbrains.annotations.NotNull;

public class BigDecimalEqualsInspection extends BaseInspection {

  @Override
  @NotNull
  public String getDisplayName() {
    return InspectionGadgetsBundle.message("big.decimal.equals.display.name");
  }

  @Override
  @NotNull
  protected String buildErrorString(Object... infos) {
    return InspectionGadgetsBundle.message("big.decimal.equals.problem.descriptor");
  }

  @Override
  public InspectionGadgetsFix buildFix(Object... infos) {
    return new BigDecimalEqualsFix();
  }

  private static class BigDecimalEqualsFix extends InspectionGadgetsFix {
    @Override
    @NotNull
    public String getFamilyName() {
      return InspectionGadgetsBundle.message("big.decimal.equals.replace.quickfix");
    }

    @Override
    public void doFix(Project project, ProblemDescriptor descriptor) {
      PsiMethodCallExpression call = PsiTreeUtil.getParentOfType(descriptor.getPsiElement(), PsiMethodCallExpression.class);
      EqualityCheck check = EqualityCheck.from(call);
      if (check == null) return;
      CommentTracker commentTracker = new CommentTracker();
      final String qualifierText = commentTracker.text(check.getLeft(), ParenthesesUtils.METHOD_CALL_PRECEDENCE);
      final String argText = commentTracker.text(check.getRight());
      String replacement = qualifierText + ".compareTo(" + argText + ")==0";
      if (!check.isLeftDereferenced() && NullnessUtil.getExpressionNullness(check.getLeft(), true) != Nullness.NOT_NULL) {
        replacement = commentTracker.text(check.getLeft(), ParenthesesUtils.EQUALITY_PRECEDENCE) + "!=null && " + replacement;
      }
      PsiReplacementUtil.replaceExpression(call, replacement, commentTracker);
    }
  }

  @Override
  public BaseInspectionVisitor buildVisitor() {
    return new BigDecimalEqualsVisitor();
  }

  private static class BigDecimalEqualsVisitor extends BaseInspectionVisitor {

    @Override
    public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
      super.visitMethodCallExpression(expression);
      EqualityCheck check = EqualityCheck.from(expression);
      if (check == null) return;
      PsiExpression left = check.getLeft();
      PsiExpression right = check.getRight();
      if (!ExpressionUtils.hasType(left, "java.math.BigDecimal")) return;
      if (!ExpressionUtils.hasType(right, "java.math.BigDecimal")) return;
      final PsiElement context = expression.getParent();
      if (context instanceof PsiExpressionStatement) {
        //cheesy, but necessary, because otherwise the quickfix will
        // produce uncompilable code (out of merely incorrect code).
        return;
      }
      registerMethodCallError(expression);
    }
  }
}
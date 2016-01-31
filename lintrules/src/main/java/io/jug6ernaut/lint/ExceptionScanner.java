package io.jug6ernaut.lint;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.JavaParser;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import lombok.ast.AstVisitor;
import lombok.ast.ConstructorInvocation;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.MethodInvocation;
import lombok.ast.Node;
import lombok.ast.Try;

import static com.android.tools.lint.detector.api.JavaContext.getParentOfType;

/**
 * Looks for assertion usages.
 */
public class ExceptionScanner extends Detector implements Detector.JavaScanner {
  /**
   * Using assertions
   */
  public static final Issue ISSUE = Issue.create(
      "Exception", //$NON-NLS-1$
      "Exceptions",
      "Exceptions Are Bad",
      Category.CORRECTNESS,
      10,
      Severity.FATAL,
      new Implementation(
          ExceptionScanner.class,
          Scope.JAVA_FILE_SCOPE));

  /**
   * Constructs a new {@link com.android.tools.lint.checks.AssertDetector} check
   */
  public ExceptionScanner() { }

  @Override
  public Speed getSpeed() {
    return Speed.FAST;
  }

  @Override
  public boolean appliesTo(@NonNull Context context, @NonNull File file) {
    return true;
  }

  @Override
  public
  List<Class<? extends Node>> getApplicableNodeTypes() {
    //noinspection unchecked
    return Arrays.<Class<? extends Node>>asList(
        MethodInvocation.class,
        ConstructorInvocation.class);
  }
  @Nullable
  @Override
  public AstVisitor createJavaVisitor(@NonNull JavaContext context) {
    return new CallVisitor(context);
  }

  private class CallVisitor extends ForwardingAstVisitor {
    private final JavaContext mContext;
    public CallVisitor(JavaContext context) {
      mContext = context;
    }

    @Override
    public boolean visitMethodInvocation(@NonNull MethodInvocation call) {
      JavaParser.ResolvedNode resolved = mContext.resolve(call);
      if (resolved instanceof JavaParser.ResolvedMethod) {
        JavaParser.ResolvedMethod method = (JavaParser.ResolvedMethod) resolved;
        checkCall(call, method);
      }
      return false;
    }

    @Override
    public boolean visitConstructorInvocation(@NonNull ConstructorInvocation call) {
      JavaParser.ResolvedNode resolved = mContext.resolve(call);
      if (resolved instanceof JavaParser.ResolvedMethod) {
        JavaParser.ResolvedMethod method = (JavaParser.ResolvedMethod) resolved;
        checkCall(call, method);
      }
      return false;
    }

    private void checkCall(@NonNull Node call, JavaParser.ResolvedMethod method) {
      boolean throwsException = method.getSignature().contains(" throws ");

      if(throwsException) {
        try {
          Try tryCatch = getParentOfType(call, Try.class);
          if (tryCatch == null) {
            mContext.report(ISSUE, getLocation(mContext, call), "Method can throw an Exception");
          }
        } catch (Exception e) {
          System.err.println("Failed: " + e.getMessage());
          e.printStackTrace();
        }
      }
    }
  }

  @NonNull
  public Location getLocation(@NonNull JavaContext context, @NonNull Node node) {
    lombok.ast.Position position = node.getPosition();
    return Location.create(context.file, context.getContents(),
        position.getStart(), position.getEnd());
  }
}
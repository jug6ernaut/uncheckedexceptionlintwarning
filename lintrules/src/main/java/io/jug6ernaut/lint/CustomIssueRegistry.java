package io.jug6ernaut.lint;

import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.Issue;

import java.util.Collections;
import java.util.List;

public class CustomIssueRegistry extends IssueRegistry {
  @Override
  public List<Issue> getIssues() {
    return Collections.singletonList(
        ExceptionScanner.ISSUE
    );
  }
}
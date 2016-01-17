/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.testing;

import java.io.PrintStream;

import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;

/**
 * A dependency visitor that dumps the graph to the console.
 */
public class ConsoleDependencyGraphDumper implements DependencyVisitor {

  private PrintStream out;

  private String currentIndent = "";

  public ConsoleDependencyGraphDumper() {
    this(null);
  }

  public ConsoleDependencyGraphDumper(PrintStream out) {
    this.out = (out != null) ? out : System.out;
  }

  public boolean visitEnter(DependencyNode node) {
    out.println(currentIndent + node);
    if (currentIndent.length() <= 0) {
      currentIndent = "+- ";
    } else {
      currentIndent = "|  " + currentIndent;
    }
    return true;
  }

  public boolean visitLeave(DependencyNode node) {
    currentIndent = currentIndent.substring(3, currentIndent.length());
    return true;
  }

}

/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.model.io;

import java.io.InputStream;
import java.util.Map;

import org.codehaus.swizzle.stream.DelimitedTokenReplacementInputStream;
import org.codehaus.swizzle.stream.StringTokenHandler;

public class InterpolatingInputStream extends DelimitedTokenReplacementInputStream {
  public InterpolatingInputStream(final InputStream in, final Map<String, String> variables) {
    super(in, "${", "}", new StringTokenHandler() {
      public String handleToken(String token) {
        Object object = variables.get(token);
        if (object != null) {
          return object.toString();
        }
        return "${" + token + "}";
      }
    });
  }
}

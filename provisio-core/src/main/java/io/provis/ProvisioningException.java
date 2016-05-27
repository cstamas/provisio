/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis;

public class ProvisioningException extends RuntimeException {

  private static final long serialVersionUID = -2662475912560280300L;

  public ProvisioningException(String message) {
    super(message);
  }

  public ProvisioningException(String message, Throwable cause) {
    super(message, cause);
  }

  public ProvisioningException(Throwable cause) {
    super(cause);
  }

}
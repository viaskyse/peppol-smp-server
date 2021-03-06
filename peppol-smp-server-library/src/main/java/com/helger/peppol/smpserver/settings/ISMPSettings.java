/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.peppol.smpserver.settings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.type.ITypedObject;

/**
 * Runtime settings for this SMP server instance.
 *
 * @author Philip Helger
 */
public interface ISMPSettings extends ITypedObject <String>
{
  /**
   * Check if the writable parts of the REST API are disabled. If this is the
   * case, only the read-only part of the API can be used. The writable REST API
   * will return an HTTP 404 error.
   *
   * @return <code>true</code> if it is disabled, <code>false</code> if it is
   *         enabled. By the default the writable API is enabled.
   */
  boolean isRESTWritableAPIDisabled ();

  /**
   * Check if the PEPPOL Directory integration (offering the /businesscard API)
   * is enabled.
   *
   * @return <code>true</code> if it is enabled, <code>false</code> otherwise.
   *         By default it is disabled.
   */
  boolean isPEPPOLDirectoryIntegrationEnabled ();

  /**
   * If the PEPPOL Directory integration is enabled, should the changes be
   * pushed automatically?
   *
   * @return <code>true</code> if it is enabled, <code>false</code> otherwise.
   *         By default it is disabled.
   */
  boolean isPEPPOLDirectoryIntegrationAutoUpdate ();

  /**
   * @return The host name of the PEPPOL Directory server. Never
   *         <code>null</code>.
   */
  @Nonnull
  String getPEPPOLDirectoryHostName ();

  /**
   * @return <code>true</code> if the SML connection is active,
   *         <code>false</code> if not.
   */
  @Deprecated
  default boolean isWriteToSML ()
  {
    return isSMLActive ();
  }

  /**
   * @return <code>true</code> if the SML connection is active,
   *         <code>false</code> if not.
   * @since 5.0.4 - renamed
   */
  boolean isSMLActive ();

  /**
   * @return <code>true</code> if the SML is needed and warnings should be
   *         emitted if it is disabled, <code>false</code> if not. Default is
   *         <code>true</code>.
   * @see #isSMLActive()
   * @since 5.0.4
   */
  boolean isSMLNeeded ();

  /**
   * @return The SML URL to use (the manage participant endpoint - e.g.
   *         <code>https://edelivery.tech.ec.europa.eu/edelivery-sml/manageparticipantidentifier</code>).
   *         Only relevant when {@link #isSMLActive()} is <code>true</code>.
   */
  @Nullable
  String getSMLURL ();
}

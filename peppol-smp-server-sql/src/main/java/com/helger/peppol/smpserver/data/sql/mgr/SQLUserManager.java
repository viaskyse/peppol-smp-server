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
package com.helger.peppol.smpserver.data.sql.mgr;

import java.util.Collection;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.string.StringHelper;
import com.helger.db.jpa.JPAExecutionResult;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.smpserver.data.sql.AbstractSMPJPAEnabledManager;
import com.helger.peppol.smpserver.data.sql.model.DBOwnership;
import com.helger.peppol.smpserver.data.sql.model.DBOwnershipID;
import com.helger.peppol.smpserver.data.sql.model.DBServiceGroup;
import com.helger.peppol.smpserver.data.sql.model.DBServiceGroupID;
import com.helger.peppol.smpserver.data.sql.model.DBUser;
import com.helger.peppol.smpserver.domain.user.ISMPUser;
import com.helger.peppol.smpserver.domain.user.ISMPUserManager;
import com.helger.peppol.smpserver.exception.SMPNotFoundException;
import com.helger.peppol.smpserver.exception.SMPUnauthorizedException;
import com.helger.peppol.smpserver.exception.SMPUnknownUserException;

/**
 * A EclipseLink based implementation of the {@link ISMPUserManager} interface.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
public final class SQLUserManager extends AbstractSMPJPAEnabledManager implements ISMPUserManager
{
  public SQLUserManager ()
  {}

  public boolean isSpecialUserManagementNeeded ()
  {
    return true;
  }

  public void createUser (@Nonnull final String sUserName, @Nonnull final String sPassword)
  {
    doInTransaction ( () -> {
      final DBUser aDBUser = new DBUser (sUserName, sPassword);
      getEntityManager ().persist (aDBUser);
    });
  }

  public void updateUser (@Nonnull final String sUserName, @Nonnull final String sPassword)
  {
    doInTransaction ( () -> {
      final DBUser aDBUser = getEntityManager ().find (DBUser.class, sUserName);
      if (aDBUser != null)
      {
        aDBUser.setPassword (sPassword);
        getEntityManager ().merge (aDBUser);
      }
    });
  }

  public void deleteUser (@Nullable final String sUserName)
  {
    if (StringHelper.hasText (sUserName))
      doInTransaction ( () -> {
        final EntityManager aEM = getEntityManager ();
        final DBUser aDBUser = aEM.find (DBUser.class, sUserName);
        if (aDBUser != null)
          aEM.remove (aDBUser);
      });
  }

  @Nonnegative
  public int getUserCount ()
  {
    JPAExecutionResult <Long> ret;
    ret = doSelect ( () -> {
      final long nCount = getSelectCountResult (getEntityManager ().createQuery ("SELECT COUNT(p) FROM DBUser p"));
      return Long.valueOf (nCount);
    });
    if (ret.hasThrowable ())
    {
      exceptionCallbacks ().forEach (x -> x.onException (ret.getThrowable ()));
      return 0;
    }
    return ret.get ().intValue ();
  }

  @Nullable
  @ReturnsMutableCopy
  public ICommonsList <ISMPUser> getAllUsers ()
  {
    JPAExecutionResult <Collection <DBUser>> ret;
    ret = doSelect ( () -> getEntityManager ().createQuery ("SELECT p FROM DBUser p", DBUser.class).getResultList ());
    if (ret.hasThrowable ())
    {
      exceptionCallbacks ().forEach (x -> x.onException (ret.getThrowable ()));
      return null;
    }
    return new CommonsArrayList <> (ret.get ());
  }

  @Nullable
  public DBUser getUserOfID (@Nullable final String sID)
  {
    if (StringHelper.hasNoText (sID))
      return null;

    JPAExecutionResult <DBUser> ret;
    ret = doSelect ( () -> getEntityManager ().find (DBUser.class, sID));
    if (ret.hasThrowable ())
    {
      exceptionCallbacks ().forEach (x -> x.onException (ret.getThrowable ()));
      return null;
    }
    return ret.get ();
  }

  @Nonnull
  public DBUser validateUserCredentials (@Nonnull final BasicAuthClientCredentials aCredentials) throws Throwable
  {
    JPAExecutionResult <DBUser> ret;
    ret = doSelect ( () -> {
      final String sUserName = aCredentials.getUserName ();
      final DBUser aDBUser = getEntityManager ().find (DBUser.class, sUserName);

      // Check that the user exists
      if (aDBUser == null)
        throw new SMPUnknownUserException (sUserName);

      // Check that the password is correct
      if (!aDBUser.getPassword ().equals (aCredentials.getPassword ()))
        throw new SMPUnauthorizedException ("Illegal password for user '" + sUserName + "'");

      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug ("Verified credentials of user '" + sUserName + "' successfully");
      return aDBUser;
    });
    return ret.getOrThrow ();
  }

  @Nonnull
  public DBOwnership verifyOwnership (@Nonnull final IParticipantIdentifier aServiceGroupID,
                                      @Nonnull final ISMPUser aCredentials) throws SMPUnauthorizedException
  {
    // Resolve service group
    // to throw a 404 if a service group does not exist
    final DBServiceGroup aServiceGroup = getEntityManager ().find (DBServiceGroup.class,
                                                                   new DBServiceGroupID (aServiceGroupID));
    if (aServiceGroup == null)
    {
      throw new SMPNotFoundException ("Service group " + aServiceGroupID.getURIEncoded () + " does not exist");
    }

    final DBOwnershipID aOwnershipID = new DBOwnershipID (aCredentials.getID (), aServiceGroupID);
    final DBOwnership aOwnership = getEntityManager ().find (DBOwnership.class, aOwnershipID);
    if (aOwnership == null)
    {
      throw new SMPUnauthorizedException ("User '" +
                                          aCredentials.getUserName () +
                                          "' does not own " +
                                          aServiceGroupID.getURIEncoded ());
    }

    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("Verified service group ID " +
                       aServiceGroupID.getURIEncoded () +
                       " is owned by user '" +
                       aCredentials.getUserName () +
                       "'");
    return aOwnership;
  }
}

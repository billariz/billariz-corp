/**
 * Copyright (C) 2025 Uppli SAS — Billariz
 *
 * This file is part of Billariz, licensed under the GNU Affero General
 * Public License v3.0 (AGPL-3.0). You may use, modify and distribute
 * this software under the terms of the AGPL-3.0.
 *
 * For commercial use without AGPL obligations, contact:
 * contact@billariz.com | contact@uppli.fr
 * https://billariz.com
 */

package com.billariz.corp.notifier;

import com.billariz.corp.database.model.User;
import com.billariz.corp.notifier.exception.NotifierException;

public interface UserNotifier
{
    void newUser(User user, String password) throws NotifierException;

    void resetUserPassword(User user, String password) throws NotifierException;
}

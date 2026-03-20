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

package com.billariz.corp.launcher.exception;

public class LauncherFatalException extends LauncherException
{
    private Object[] args;

    public LauncherFatalException(String message, Object[] args)
    {
        super(message);
        this.args = args;
    }

    public LauncherFatalException(String message)
    {
        super(message);
    }

    public Object[] getArgs()
    {
        return args;
    }
}

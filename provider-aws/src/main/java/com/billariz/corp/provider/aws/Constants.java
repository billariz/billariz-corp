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

package com.billariz.corp.provider.aws;

import com.billariz.corp.provider.BaseConstants;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants
{
    public static final String PREFIX_CONF   = BaseConstants.CONFIG_BASE + ".aws";

    public static final String PROVIDER_NAME = "provider-aws";
}

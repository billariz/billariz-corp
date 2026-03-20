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

package com.billariz.corp.notifier.utils;

import java.util.Collections;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Component;

@Component
public class DefaultPasswordGeneratorImpl implements PasswordGenerator
{
    @Override
    public String generate()
    {
        var upperCaseLetters = RandomStringUtils.random(3, 65, 90, true, true);
        var lowerCaseLetters = RandomStringUtils.random(2, 97, 122, true, true);
        var numbers = RandomStringUtils.randomNumeric(3);
        var totalChars = RandomStringUtils.randomAlphanumeric(2);
        var combinedChars = upperCaseLetters.concat(lowerCaseLetters).concat(numbers).concat(totalChars);
        var pwdChars = combinedChars.chars().mapToObj(c -> (char) c).collect(Collectors.toList());

        Collections.shuffle(pwdChars);
        return pwdChars.stream().collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();
    }
}

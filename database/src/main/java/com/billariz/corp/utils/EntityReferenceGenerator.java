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

package com.billariz.corp.utils;

import java.time.LocalDate;
import javax.transaction.Transactional;
import org.springframework.stereotype.Component;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.repository.ParameterRepository;
import com.billariz.corp.database.repository.SequenceManagerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class EntityReferenceGenerator {

    private final ParameterRepository parameterRepository;

    private final SequenceManagerRepository sequenceManagerRepository;

    public String referenceGenerator(ObjectType object) {
        // Récupérer le modèle depuis la configuration (base de données )
        var template = parameterRepository.findAllByTypeAndNameAndStartDateBefore("MASTER",getTemplateParam(object),LocalDate.now()).get(0).getValue();
        // template => "{PREFIX}-{YEAR}-{MONTH}-{SEQ:5}";
        // Récupérer le préfixe 
        var prefix = parameterRepository.findAllByTypeAndNameAndStartDateBefore("MASTER",getPrefixParam(object),LocalDate.now()).get(0).getValue();
        Long sequence = getNextSequence(object);
        // Analyse du modèle pour extraire la longueur de la séquence
        int start = template.indexOf("{SEQ:") + 5;
        int end = template.indexOf("}", start);
        int sequenceLength = Integer.parseInt(template.substring(start, end));
    
        // Générer les valeurs dynamiques
        String year = String.valueOf(LocalDate.now().getYear());
        String month = String.format("%02d", LocalDate.now().getMonthValue());
        String seq = String.format("%0" + sequenceLength + "d", sequence);
    
        // Remplacer les valeurs dans le modèle
        return template.replace("{PREFIX}", prefix)
                       .replace("{YEAR}", year)
                       .replace("{MONTH}", month)
                       .replaceAll("\\{SEQ:[0-9]+\\}", seq);
    }

    private String getTemplateParam(ObjectType object) {
        switch (object) {
            case BILL:
                return "invoiceReferenceTemplate";
            case CALCUL:
                return "calculReferenceTemplate";
            case CUSTOMER:
                return "customerReferenceTemplate";
            case CONTRACT:
                return "contractReferenceTemplate";
            case PERIMETER:
                return "perimeterReferenceTemplate";
            case POINT_OF_SERVICE:
                return "posReferenceTemplate";
            default:
                break;
        }
        return "";
    }

    private String getPrefixParam(ObjectType object) {
        switch (object) {
            case BILL:
                return "invoiceReferencePrefix";
            case CALCUL:
                return "calculReferencePrefix";
            case CUSTOMER:
                return "customerReferencePrefix";
            case CONTRACT:
                return "contractReferencePrefix";
            case PERIMETER:
                return "perimeterReferencePrefix";
            case POINT_OF_SERVICE:
                return "posReferencePrefix";
            default:
                break;
        }
        return "";
    }

    @Transactional
    public Long getNextSequence(ObjectType sequenceName) {
        // Récupérer la dernière valeur et mettre à jour
        var sequence = sequenceManagerRepository.findBySequenceName(sequenceName.getValue())
                .orElseThrow(() -> new RuntimeException("Sequence not found to create object reference: " + sequenceName));

        Long nextValue = sequence.getLastValue() + 1;
        sequence.setLastValue(nextValue);

        // Sauvegarder la nouvelle valeur
        sequenceManagerRepository.save(sequence);
        return nextValue;   
    }
    
}

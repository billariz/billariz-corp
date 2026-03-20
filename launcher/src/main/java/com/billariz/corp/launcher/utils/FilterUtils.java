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

package com.billariz.corp.launcher.utils;

import java.time.LocalDate;
import java.util.Objects;
import com.billariz.corp.database.model.BillDetail;
import com.billariz.corp.database.model.Contract;
import com.billariz.corp.database.model.Customer;
import com.billariz.corp.database.model.ObjectProcessRule;
import com.billariz.corp.database.model.PointOfService;
import com.billariz.corp.database.model.PointOfServiceConfiguration;
import com.billariz.corp.database.model.Rate;
import com.billariz.corp.database.model.Service;
import com.billariz.corp.database.model.ServiceStartOption;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FilterUtils
{

    public static boolean filter(String filter, String value)
    {
        if ("*".equals(filter))
            return true;
        return Objects.equals(filter, value);
    }

    public static boolean filterIsAfter(LocalDate filter, LocalDate value)
    {
        if (filter == null)
            return true;
        return filter.isAfter(value);
    }

    public static boolean filterIsBefore(LocalDate filter, LocalDate value)
    {
        if (filter == null)
            return true;
        return filter.isBefore(value);
    }

    public static boolean filterRule(ObjectProcessRule filter, Contract contract, String newStatus, String customerCategory)
    {
        return filter.getNewStatus().equals(newStatus) && filter(filter.getInitialStatus(), contract.getStatus())
                && filter(filter.getMarket(), contract.getMarket()) && filter(filter.getCustomerCategory(), customerCategory)
                && filter(filter.getChannel(), contract.getChannel()) && filter(filter.getSeller(), contract.getSeller())
                && filter(filter.getDirection(), contract.getDirection()) && filter(filter.getServiceCategory(), contract.getServiceCategory())
                && filter(filter.getServiceSubCategory(), contract.getServiceSubCategory());
    }

    public static boolean filter(ServiceStartOption filter, String paymentMode, PointOfService pos, PointOfServiceConfiguration posConf, Customer customer, Contract contract, Service service)
    {
        return filterService(filter, service) && filterPaymentMode(filter, paymentMode) && filterPointOfService(filter, pos, posConf)
                && filterCustomer(filter, customer) && filterContract(filter, contract);
    }

    private static boolean filterService(ServiceStartOption filter, Service service)
    {
        if (service == null)
            return true;
        return filter(filter.getConsumptionThreshold(), service.getThreshold()) 
                    && (service.getDirection() == null || filter(filter.getDirection(), service.getDirection()))
                    && (service.getTouGroup()==null || filter(filter.getTouGroup(), service.getTouGroup()));
    }

    private static boolean filterPaymentMode(ServiceStartOption filter, String paymentMode)
    {
        return filter(filter.getPaymentMode(), paymentMode);
    }

    private static boolean filterPointOfService(ServiceStartOption filter, PointOfService pos, PointOfServiceConfiguration posConf)
    {
        return filter(filter.getPosCategory(), posConf.getPosCategory()) 
                && filter(filter.getDgoCode(), pos.getDgoCode())
                && filter(filter.getTouGroup(), posConf.getTouGroup())
                && filter(filter.getTgoCode(), pos.getTgoCode());
    }

    private static boolean filterCustomer(ServiceStartOption filter, Customer customer)
    {
        return filter(filter.getCustomerCategory(), customer.getCategory()) && filter(filter.getCustomerType(), customer.getType());

    }

    private static boolean filterContract(ServiceStartOption filter, Contract contract)
    {
        return (contract.getDirection() == null || filter(filter.getDirection(), contract.getDirection()))
                && filter(filter.getChannel(), contract.getChannel())
                && filter(filter.getSeller(), contract.getSeller()) 
                && filter(filter.getServiceCategory(), contract.getServiceCategory())
                && filter(filter.getServiceSubCategory(), contract.getServiceSubCategory()) 
                && filter(filter.getMarket(), contract.getMarket())
                && filter(filter.getBillingMode(), contract.getBillingMode())
                && filterIsBefore(filter.getStartDate(), contract.getContractualStartDate())
                && filterIsAfter(filter.getEndDate(), contract.getContractualEndDate());
    }

    public static boolean filterRate(Rate filter, BillDetail billDetail)
    {
        return filterIsBefore(filter.getStartDate(), billDetail.getStartDate()) && filterIsAfter(filter.getEndDate(), billDetail.getEndDate());
    }
}

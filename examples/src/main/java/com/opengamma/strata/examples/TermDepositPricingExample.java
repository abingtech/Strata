/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;

import java.time.LocalDate;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.CalculationRunner;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.Results;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.examples.marketdata.ExampleData;
import com.opengamma.strata.examples.marketdata.ExampleMarketData;
import com.opengamma.strata.examples.marketdata.ExampleMarketDataBuilder;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.StandardComponents;
import com.opengamma.strata.product.AttributeType;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.deposit.TermDeposit;
import com.opengamma.strata.product.deposit.TermDepositTrade;
import com.opengamma.strata.report.ReportCalculationResults;
import com.opengamma.strata.report.trade.TradeReport;
import com.opengamma.strata.report.trade.TradeReportTemplate;

/**
 * Example to illustrate说明 using the engine to price定价 a Term Deposit定期存款.
 * <p>
 * This makes use of the example engine and the example market data environment.
 * 基于引擎和市场数据的环境
 */
public class TermDepositPricingExample {

  /**
   * Runs the example, pricing the instruments对工具进行定价, producing the output as an ASCII table.
   * 
   * @param args  ignored
   */
  public static void main(String[] args) {
    // setup calculation runner component, which needs life-cycle management
    // a typical application might use dependency injection to obtain the instance
    try (CalculationRunner runner = CalculationRunner.ofMultiThreaded()) {
      calculate(runner);
    }
  }

  // obtains the data and calculates the grid of results
  private static void calculate(CalculationRunner runner) {
    // the trades that will have measures calculated
    // todo: 创建两笔交易
    List<Trade> trades = ImmutableList.of(createTrade1(), createTrade2());

    // the columns, specifying the measures to be calculated
    // todo: 计算列，指定需要计算的指标
    List<Column> columns = ImmutableList.of(
        Column.of(Measures.PRESENT_VALUE),// todo: 现值，表示这笔存款现在值多少钱
        Column.of(Measures.PV01_CALIBRATED_SUM),// todo: PV01
        Column.of(Measures.PAR_RATE),
        Column.of(Measures.PAR_SPREAD),
        Column.of(Measures.PV01_CALIBRATED_BUCKETED));

    // use the built-in example market data
    // todo: 指定估值日期，交易发生日？1月22做的交易？
    LocalDate valuationDate = LocalDate.of(2014, 1, 22);
    // todo: 创建一个市场数据构建器
    ExampleMarketDataBuilder marketDataBuilder = ExampleMarketData.builder();
    // todo: 利用构建器构造一份估值日期对应的市场数据快照
    MarketData marketData = marketDataBuilder.buildSnapshot(valuationDate);

    // the complete set of rules for calculating measures
    // todo: 计算函数集合
    CalculationFunctions functions = StandardComponents.calculationFunctions();
    // todo: 计算函数与计算参数组合成计算规则, 这里的计算参数是市场利率
    CalculationRules rules = CalculationRules.of(functions, marketDataBuilder.ratesLookup(valuationDate));

    // the reference data, such as holidays and securities
    // todo: 标准的引用数据
    ReferenceData refData = ReferenceData.standard();

    // calculate the results
    // todo: 计算并返回收集的结果
    Results results = runner.calculate(rules, trades, columns, marketData, refData);

    // use the report runner to transform the engine results into a trade report
    ReportCalculationResults calculationResults =
        ReportCalculationResults.of(valuationDate, trades, columns, results, functions, refData);
    // todo: 交易报告的模板
    TradeReportTemplate reportTemplate = ExampleData.loadTradeReportTemplate("term-deposit-report-template");
    TradeReport tradeReport = TradeReport.of(calculationResults, reportTemplate);
    tradeReport.writeAsciiTable(System.out);
  }

  //-----------------------------------------------------------------------  
  // create a TermDeposit trade
  private static Trade createTrade1() {
    // todo: 9月12到12月12，买入，
    TermDeposit td = TermDeposit.builder()
        .buySell(BuySell.BUY)
        .startDate(LocalDate.of(2014, 9, 12))
        .endDate(LocalDate.of(2014, 12, 12))
        .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, HolidayCalendarIds.GBLO))
        .currency(Currency.USD)
        .notional(10_000_000) // 1000w
        .dayCount(DayCounts.THIRTY_360_ISDA) // 计息天数：一年按360天计算，一个月按30天，遇到31号需要特殊调整
        .rate(0.003)
        .build();

    return TermDepositTrade.builder()
        .product(td)
            // 对一笔交易添加额外的信息
        .info(TradeInfo.builder()
            .id(StandardId.of("example", "11"))
            .addAttribute(AttributeType.DESCRIPTION, "Deposit 10M at 3%")
            .counterparty(StandardId.of("example", "AA"))
                // 交割日期
            .settlementDate(LocalDate.of(2014, 12, 16))
            .build())
        .build();
  }

  // create a TermDeposit trade
  private static Trade createTrade2() {
    TermDeposit td = TermDeposit.builder()
        .buySell(BuySell.BUY)
        .startDate(LocalDate.of(2014, 12, 12))
        .endDate(LocalDate.of(2015, 12, 12))
        .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, HolidayCalendarIds.GBLO))
        .currency(Currency.USD)
        .notional(5_000_000)
        .dayCount(DayCounts.THIRTY_360_ISDA)
        .rate(0.0038)
        .build();

    return TermDepositTrade.builder()
        .product(td)
        .info(TradeInfo.builder()
            .id(StandardId.of("example", "22"))
            .addAttribute(AttributeType.DESCRIPTION, "Deposit 5M at 3.8%")
            .counterparty(StandardId.of("example", "AA"))
            .settlementDate(LocalDate.of(2015, 12, 16))
            .build())
        .build();
  }

}

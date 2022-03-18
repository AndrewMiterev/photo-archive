package com.example.photoarchive.components;

import com.example.photoarchive.services.FileMetaService;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.AxisType;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.Configuration;
import com.vaadin.flow.component.charts.model.DataLabels;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.charts.model.DataSeriesItem;
import com.vaadin.flow.component.charts.model.DataSeriesItemTimeline;
import com.vaadin.flow.component.charts.model.MarkerSymbolEnum;
import com.vaadin.flow.component.charts.model.PlotOptionsTimeline;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.security.PermitAll;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@PermitAll
@Route(value = "dashboard", layout = MainAppLayout.class)
public class DashboardView extends Div implements RouterLayout {
	private final FileMetaService service;

	public DashboardView(FileMetaService service) {
		this.service = service;
//
//		VerticalLayout layout = new VerticalLayout();
//		add(layout);
//		layout.setAlignItems(FlexComponent.Alignment.CENTER);
//		layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
//
//		layout.add(new H1("Dashboard"));

		var clearStatistics = service.getPhotosCountByDate();
		var preparedStatistics = photosStatisticsWith0(clearStatistics);

		Chart chartSPLine = new Chart(ChartType.SPLINE);
		chartSPLine.setTimeline(true);

		Configuration configuration = chartSPLine.getConfiguration();
		configuration.getTooltip().setEnabled(true);	// подписываем точку на графике

		DataSeries dataSeries = new DataSeries();
		dataSeries.setName("Photos by date");

		preparedStatistics.forEach(data -> dataSeries.add(new DataSeriesItem(
				data.getLeft().atStartOfDay().toInstant(ZoneOffset.UTC),
				data.getValue()))
		);

		configuration.addSeries(dataSeries);
		add(chartSPLine);

		Chart chartTimeLine = new Chart(ChartType.TIMELINE);

		Configuration conf = chartTimeLine.getConfiguration();
		// вывод информации при наведение на точку
		conf.getTooltip().setEnabled(true);

		DataSeries series = new DataSeries();
		for (var data : clearStatistics) {
			var item = new DataSeriesItemTimeline(
					data.getLeft().atStartOfDay().toInstant(ZoneOffset.UTC),
					"geoInfo.locality",
					"geoInfo.country",
					"geoInfo.address"
			);
			item.setCursor("Photos count #%s".formatted(data.getValue()));
			series.add(item);
		}

		PlotOptionsTimeline options = new PlotOptionsTimeline();
		options.getMarker().setSymbol(MarkerSymbolEnum.DIAMOND);
		DataLabels labels = options.getDataLabels();
		labels.setAllowOverlap(false);
		labels.setFormat("<span style=\"font-weight: bold;color:{point.color}\" > {point.x:%d %b %Y}</span><br/>{point.label}<br/>{point.cursor}");
		labels.setShadow(true);

		series.setPlotOptions(options);
		conf.addSeries(series);

		// Configure the axes
		conf.getxAxis().setVisible(true);
		conf.getxAxis().setType(AxisType.DATETIME);
		conf.getyAxis().setVisible(false);

		add(chartTimeLine);
	}

	private LocalDate collect0(List<Pair<LocalDate, Integer>> list, LocalDate date1, LocalDate date2) {
		if (Objects.isNull(date1)) {
			assert date2 != null;
			date1 = date2;
			list.add(Pair.of(date1.minusDays(1), 0));
		}
		if (date2.isAfter(date1.plusDays(1))) list.add(Pair.of(date1.plusDays(1), 0));
		if (date2.isAfter(date1.plusDays(2))) list.add(Pair.of(date2.minusDays(1), 0));
		return date2;
	}

	private List<Pair<LocalDate, Integer>> photosStatisticsWith0(List<Pair<LocalDate, Integer>> in) {
		List<Pair<LocalDate, Integer>> listWith0 = new ArrayList<>();
		LocalDate date = null;
		for (var pair : in) {
			date = collect0(listWith0, date, pair.getKey());
			listWith0.add(pair);
		}
		collect0(listWith0, date, LocalDate.now());
		return listWith0;
	}
}

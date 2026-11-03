package com.runnit.api.service;

import com.garmin.fit.Decode;
import com.garmin.fit.MesgBroadcaster;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.RecordMesgListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Converts Garmin GPS samples and activity files to the encoded route format
 * already consumed by Runnit's map components. Garmin can deliver a route as
 * JSON activity samples or as a FIT/GPX/TCX file callback.
 */
@Slf4j
@Service
public class GarminRouteParser {

    private static final int MAX_ROUTE_POINTS = 20_000;
    private static final double FIT_SEMICIRCLE_TO_DEGREES = 180d / 2_147_483_648d;

    public RouteData parseSamples(Object samples) {
        if (!(samples instanceof Collection<?> collection)) return RouteData.empty();
        List<Point> points = new ArrayList<>();
        for (Object sample : collection) {
            if (!(sample instanceof Map<?, ?> map)) continue;
            Double lat = number(map, "latitudeInDegree", "latitude", "lat");
            Double lng = number(map, "longitudeInDegree", "longitude", "lng", "lon");
            Double elevation = number(map, "elevationInMeters", "elevation", "altitudeInMeters", "altitude");
            addPoint(points, lat, lng, elevation);
        }
        return toRouteData(points);
    }

    public RouteData parseFile(byte[] bytes, String fileType) {
        if (bytes == null || bytes.length == 0) return RouteData.empty();
        String type = fileType == null ? "" : fileType.toLowerCase(Locale.ROOT);
        try {
            if (type.contains("fit") || looksLikeFit(bytes)) return parseFit(bytes);
            if (type.contains("tcx")) return parseTcx(bytes);
            if (type.contains("gpx")) return parseGpx(bytes);

            // Garmin has used a few content-type/file-type variants. Try the
            // text formats as a safe fallback when the ping omits fileType.
            String text = new String(bytes, StandardCharsets.UTF_8).trim();
            if (text.startsWith("<")) {
                if (text.toLowerCase(Locale.ROOT).contains("<trainingcenterdatabase")) return parseTcx(bytes);
                return parseGpx(bytes);
            }
            return parseFit(bytes);
        } catch (Exception e) {
            log.warn("Unable to parse Garmin activity file as {}: {}", fileType, e.getMessage());
            return RouteData.empty();
        }
    }

    private RouteData parseFit(byte[] bytes) {
        List<Point> points = new ArrayList<>();
        Decode decode = new Decode();
        MesgBroadcaster broadcaster = new MesgBroadcaster(decode);
        broadcaster.addListener((RecordMesgListener) record -> {
            Integer semicircleLat = record.getPositionLat();
            Integer semicircleLng = record.getPositionLong();
            if (semicircleLat == null || semicircleLng == null) return;
            addPoint(points,
                    semicircleLat * FIT_SEMICIRCLE_TO_DEGREES,
                    semicircleLng * FIT_SEMICIRCLE_TO_DEGREES,
                    record.getAltitude() == null ? null : record.getAltitude().doubleValue());
        });
        if (!decode.read(new ByteArrayInputStream(bytes), broadcaster)) return RouteData.empty();
        return toRouteData(points);
    }

    private RouteData parseGpx(byte[] bytes) throws Exception {
        Document document = parseXml(bytes);
        NodeList trackPoints = document.getElementsByTagNameNS("*", "trkpt");
        if (trackPoints.getLength() == 0) trackPoints = document.getElementsByTagName("trkpt");
        if (trackPoints.getLength() == 0) trackPoints = document.getElementsByTagNameNS("*", "rtept");
        if (trackPoints.getLength() == 0) trackPoints = document.getElementsByTagName("rtept");

        List<Point> points = new ArrayList<>();
        for (int i = 0; i < trackPoints.getLength(); i++) {
            Node node = trackPoints.item(i);
            if (!(node instanceof Element point)) continue;
            Double lat = attribute(point, "lat");
            Double lng = attribute(point, "lon");
            Double elevation = childNumber(point, "ele");
            addPoint(points, lat, lng, elevation);
        }
        return toRouteData(points);
    }

    private RouteData parseTcx(byte[] bytes) throws Exception {
        Document document = parseXml(bytes);
        NodeList trackPoints = document.getElementsByTagNameNS("*", "Trackpoint");
        if (trackPoints.getLength() == 0) trackPoints = document.getElementsByTagName("Trackpoint");

        List<Point> points = new ArrayList<>();
        for (int i = 0; i < trackPoints.getLength(); i++) {
            Node node = trackPoints.item(i);
            if (!(node instanceof Element point)) continue;
            Element position = child(point, "Position");
            if (position == null) continue;
            Double lat = childNumber(position, "LatitudeDegrees");
            Double lng = childNumber(position, "LongitudeDegrees");
            Double elevation = childNumber(point, "AltitudeMeters");
            addPoint(points, lat, lng, elevation);
        }
        return toRouteData(points);
    }

    private Document parseXml(byte[] bytes) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(new String(bytes, StandardCharsets.UTF_8))));
    }

    private boolean looksLikeFit(byte[] bytes) {
        return bytes.length >= 12 && bytes[8] == '.' && bytes[9] == 'F' && bytes[10] == 'I' && bytes[11] == 'T';
    }

    private RouteData toRouteData(List<Point> points) {
        if (points.isEmpty()) return RouteData.empty();
        StringBuilder polyline = new StringBuilder(points.size() * 4);
        long previousLat = 0;
        long previousLng = 0;
        for (Point point : points) {
            long lat = Math.round(point.latitude() * 100_000d);
            long lng = Math.round(point.longitude() * 100_000d);
            appendEncoded(polyline, lat - previousLat);
            appendEncoded(polyline, lng - previousLng);
            previousLat = lat;
            previousLng = lng;
        }
        Point first = points.get(0);
        return new RouteData(polyline.toString(), first.latitude(), first.longitude());
    }

    private void addPoint(List<Point> points, Double lat, Double lng, Double elevation) {
        if (lat == null || lng == null || !Double.isFinite(lat) || !Double.isFinite(lng)
                || lat < -90 || lat > 90 || lng < -180 || lng > 180) return;
        if (points.size() >= MAX_ROUTE_POINTS) return;
        Point next = new Point(lat, lng, elevation);
        if (!points.isEmpty() && points.get(points.size() - 1).sameCoordinate(next)) return;
        points.add(next);
    }

    private void appendEncoded(StringBuilder output, long value) {
        long encoded = value < 0 ? ~(value << 1) : value << 1;
        while (encoded >= 0x20) {
            output.append((char) ((int) ((encoded & 0x1f) | 0x20) + 63));
            encoded >>= 5;
        }
        output.append((char) ((int) encoded + 63));
    }

    private Double number(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof Number number) return number.doubleValue();
            if (value != null) try { return Double.parseDouble(value.toString()); } catch (NumberFormatException ignored) { }
        }
        return null;
    }

    private Double attribute(Element element, String name) {
        String value = element.getAttribute(name);
        if (value == null || value.isBlank()) return null;
        try { return Double.parseDouble(value); } catch (NumberFormatException ignored) { return null; }
    }

    private Double childNumber(Element parent, String localName) {
        Element child = child(parent, localName);
        if (child == null || child.getTextContent() == null) return null;
        try { return Double.parseDouble(child.getTextContent().trim()); } catch (NumberFormatException ignored) { return null; }
    }

    private Element child(Element parent, String localName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element element
                    && (localName.equals(element.getLocalName()) || localName.equals(element.getNodeName()))) return element;
        }
        return null;
    }

    public record RouteData(String polyline, Double startLat, Double startLng) {
        public static RouteData empty() { return new RouteData(null, null, null); }
        public boolean hasRoute() { return polyline != null && !polyline.isBlank(); }
    }

    private record Point(double latitude, double longitude, Double elevation) {
        private boolean sameCoordinate(Point other) {
            return Math.abs(latitude - other.latitude) < 0.0000001
                    && Math.abs(longitude - other.longitude) < 0.0000001;
        }
    }
}

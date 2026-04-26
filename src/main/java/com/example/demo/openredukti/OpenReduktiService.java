package com.example.demo.openredukti;

import com.example.demo.openredukti.dto.BootstrapRequest;
import com.example.demo.openredukti.dto.CurveInstrument;
import com.example.demo.openredukti.dto.SwapValuationRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;


@Service
public class OpenReduktiService {

    private final ObjectMapper mapper = new ObjectMapper();

    // -----------------------------------------------------------------------
    // Version
    // -----------------------------------------------------------------------

    public JsonNode getVersion() throws Exception {
        if (!OpenReduktiNative.isAvailable()) {
            return mapper.readTree("{\"status\":\"STUB\",\"message\":\"Native library not loaded – running without libopenredukti_jni.so\"}");
        }
        String json = OpenReduktiNative.getVersion();
        return mapper.readTree(json);
    }

    // -----------------------------------------------------------------------
    // Bootstrap
    // -----------------------------------------------------------------------

    public JsonNode bootstrapCurves(BootstrapRequest request) throws Exception {
        if (!OpenReduktiNative.isAvailable()) {
            throw new OpenReduktiException("Native library not available – deploy with libopenredukti_jni.so on java.library.path");
        }
        String requestJson = buildBootstrapJson(request);
        String responseJson = OpenReduktiNative.bootstrapCurves(requestJson);
        JsonNode result = mapper.readTree(responseJson);
        if ("ERROR".equals(result.path("status").asText())) {
            throw new OpenReduktiException(result.path("message").asText("bootstrap failed"));
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Valuation
    // -----------------------------------------------------------------------

    public JsonNode valueCashflows(SwapValuationRequest request) throws Exception {
        if (!OpenReduktiNative.isAvailable()) {
            throw new OpenReduktiException("Native library not available – deploy with libopenredukti_jni.so on java.library.path");
        }
        String requestJson = buildValuationJson(request);
        String responseJson = OpenReduktiNative.valueCashflows(requestJson);
        JsonNode result = mapper.readTree(responseJson);
        if ("ERROR".equals(result.path("status").asText())) {
            throw new OpenReduktiException(result.path("message").asText("valuation failed"));
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // JSON builders (avoid pulling in a heavyweight JSON-builder lib)
    // -----------------------------------------------------------------------

    private String buildBootstrapJson(BootstrapRequest req) throws Exception {
        ObjectNode root = mapper.createObjectNode();
        root.put("curveDate", req.getCurveDate());
        root.put("currency", req.getCurrency() != null ? req.getCurrency() : "USD");
        root.put("interpolatorType",
                req.getInterpolatorType() != null ? req.getInterpolatorType() : "MONOTONE_CONVEX");

        com.fasterxml.jackson.databind.node.ArrayNode instruments = mapper.createArrayNode();
        if (req.getInstruments() != null) {
            for (CurveInstrument inst : req.getInstruments()) {
                ObjectNode node = mapper.createObjectNode();
                node.put("maturityDate", inst.getMaturityDate());
                node.put("parRate", inst.getParRate());
                node.put("instrumentType",
                        inst.getInstrumentType() != null ? inst.getInstrumentType() : "SWAP");
                instruments.add(node);
            }
        }
        root.set("instruments", instruments);
        return mapper.writeValueAsString(root);
    }

    private String buildValuationJson(SwapValuationRequest req) throws Exception {
        ObjectNode root = mapper.createObjectNode();
        root.put("tradeDate", req.getTradeDate());
        root.put("currency", req.getCurrency() != null ? req.getCurrency() : "USD");
        root.put("notional", req.getNotional());
        root.put("fixedRate", req.getFixedRate());
        root.put("payFixed", req.isPayFixed());
        root.put("effectiveDate", req.getEffectiveDate());
        root.put("terminationDate", req.getTerminationDate());
        root.put("derivativeOrder", req.getDerivativeOrder());

        if (req.getZeroCurve() != null) {
            root.set("zeroCurve", mapper.valueToTree(req.getZeroCurve()));
        }
        return mapper.writeValueAsString(root);
    }
}

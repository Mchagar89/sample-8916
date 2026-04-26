package com.example.demo.openredukti;

import com.example.demo.openredukti.dto.BootstrapRequest;
import com.example.demo.openredukti.dto.SwapValuationRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/redukti")
public class OpenReduktiController {

    private final OpenReduktiService service;

    public OpenReduktiController(OpenReduktiService service) {
        this.service = service;
    }

    /**
     * GET /api/redukti/version
     * Returns the OpenRedukti library version info.
     */
    @GetMapping("/version")
    public ResponseEntity<JsonNode> version() {
        try {
            return ResponseEntity.ok(service.getVersion());
        } catch (Exception e) {
            return errorResponse(e);
        }
    }

    /**
     * POST /api/redukti/bootstrap
     * Bootstrap zero curves from par-rate market data.
     *
     * Request body example:
     * {
     *   "curveDate": "2024-01-15",
     *   "currency": "USD",
     *   "interpolatorType": "MONOTONE_CONVEX",
     *   "instruments": [
     *     { "maturityDate": "2024-04-15", "parRate": 0.0535, "instrumentType": "DEPOSIT" },
     *     { "maturityDate": "2025-01-15", "parRate": 0.0545, "instrumentType": "SWAP" },
     *     { "maturityDate": "2027-01-15", "parRate": 0.0555, "instrumentType": "SWAP" },
     *     { "maturityDate": "2029-01-15", "parRate": 0.0560, "instrumentType": "SWAP" }
     *   ]
     * }
     */
    @PostMapping("/bootstrap")
    public ResponseEntity<JsonNode> bootstrap(@RequestBody BootstrapRequest request) {
        try {
            return ResponseEntity.ok(service.bootstrapCurves(request));
        } catch (OpenReduktiException e) {
            return ResponseEntity.unprocessableEntity()
                    .body(errorNode(e.getMessage()));
        } catch (Exception e) {
            return errorResponse(e);
        }
    }

    /**
     * POST /api/redukti/value
     * Compute present value and sensitivities for a plain vanilla IRS.
     *
     * Request body example:
     * {
     *   "tradeDate":       "2024-01-15",
     *   "effectiveDate":   "2024-01-17",
     *   "terminationDate": "2029-01-17",
     *   "currency":        "USD",
     *   "notional":        1000000.0,
     *   "fixedRate":       0.055,
     *   "payFixed":        true,
     *   "derivativeOrder": 1,
     *   "zeroCurve": {
     *     "id": 1,
     *     "points": [
     *       { "maturityDate": "2024-04-17", "discountFactor": 0.9868 },
     *       { "maturityDate": "2025-01-17", "discountFactor": 0.9480 },
     *       { "maturityDate": "2027-01-17", "discountFactor": 0.8700 },
     *       { "maturityDate": "2029-01-17", "discountFactor": 0.7980 }
     *     ]
     *   }
     * }
     */
    @PostMapping("/value")
    public ResponseEntity<JsonNode> value(@RequestBody SwapValuationRequest request) {
        try {
            return ResponseEntity.ok(service.valueCashflows(request));
        } catch (OpenReduktiException e) {
            return ResponseEntity.unprocessableEntity()
                    .body(errorNode(e.getMessage()));
        } catch (Exception e) {
            return errorResponse(e);
        }
    }

    // -----------------------------------------------------------------------

    private ResponseEntity<JsonNode> errorResponse(Exception e) {
        return ResponseEntity.internalServerError().body(errorNode(e.getMessage()));
    }

    private JsonNode errorNode(String msg) {
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .createObjectNode()
                .put("status", "ERROR")
                .put("message", msg != null ? msg : "internal error");
    }
}

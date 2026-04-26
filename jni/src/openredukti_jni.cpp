/*
 * JNI bridge between Java/Spring-Boot and the OpenRedukti C++ library.
 *
 * Data exchange contract
 * ----------------------
 *  All public JNI methods accept and return UTF-8 JSON strings so that the
 *  Java side needs no protobuf runtime.  Internally, this file converts JSON
 *  ↔ OpenRedukti protobuf messages and calls the library's RequestProcessor.
 *
 * Protobuf field names
 * --------------------
 *  Field names below are taken from the OpenRedukti proto definitions.
 *  If a build error occurs on a generated accessor, cross-check with the
 *  *.pb.h files produced in ${OPENREDUKTI_BUILD_DIR}/proto/redukti/.
 */

#include <jni.h>

#include <algorithm>
#include <memory>
#include <stdexcept>
#include <string>

// OpenRedukti public C++ headers
#include "redukti/request_processor.h"

// Generated protobuf headers (produced when OpenRedukti is built with CMake)
#include "redukti/bootstrap.pb.h"
#include "redukti/cashflow.pb.h"
#include "redukti/curve.pb.h"
#include "redukti/enums.pb.h"
#include "redukti/valuation.pb.h"

// nlohmann/json – fetched at configure time into ${CMAKE_BINARY_DIR}/nlohmann/
#include "json.hpp"

using json = nlohmann::json;

// ---------------------------------------------------------------------------
// Module-level state
// ---------------------------------------------------------------------------

static std::unique_ptr<redukti::RequestProcessor> g_processor;

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

static std::string jstr_to_std(JNIEnv *env, jstring js)
{
    if (!js) return {};
    const char *p = env->GetStringUTFChars(js, nullptr);
    std::string s(p);
    env->ReleaseStringUTFChars(js, p);
    return s;
}

static jstring to_jstring(JNIEnv *env, const std::string &s)
{
    return env->NewStringUTF(s.c_str());
}

static jstring error_json(JNIEnv *env, const std::string &msg)
{
    json j = {{"status", "ERROR"}, {"message", msg}};
    return to_jstring(env, j.dump());
}

// Convert "YYYY-MM-DD" → integer YYYYMMDD that OpenRedukti uses for dates.
static int32_t parse_date(const std::string &s)
{
    std::string d = s;
    d.erase(std::remove(d.begin(), d.end(), '-'), d.end());
    return std::stoi(d);
}

// Convert integer YYYYMMDD → "YYYY-MM-DD" string.
static std::string format_date(int32_t d)
{
    char buf[11];
    int y = d / 10000, m = (d % 10000) / 100, day = d % 100;
    snprintf(buf, sizeof(buf), "%04d-%02d-%02d", y, m, day);
    return std::string(buf);
}

static redukti::Currency parse_currency(const std::string &s)
{
    if (s == "USD") return redukti::Currency::USD;
    if (s == "EUR") return redukti::Currency::EUR;
    if (s == "GBP") return redukti::Currency::GBP;
    if (s == "JPY") return redukti::Currency::JPY;
    if (s == "CHF") return redukti::Currency::CHF;
    if (s == "AUD") return redukti::Currency::AUD;
    if (s == "CAD") return redukti::Currency::CAD;
    return redukti::Currency::USD;
}

static redukti::InterpolatorType parse_interpolator(const std::string &s)
{
    if (s == "LINEAR")          return redukti::InterpolatorType::LINEAR;
    if (s == "LOG_LINEAR")      return redukti::InterpolatorType::LOG_LINEAR;
    if (s == "CUBIC_SPLINE")    return redukti::InterpolatorType::CUBIC_SPLINE;
    if (s == "LOG_CUBIC_SPLINE") return redukti::InterpolatorType::LOG_CUBIC_SPLINE;
    // Default: MonotoneConvex is preferred for interest-rate curves.
    return redukti::InterpolatorType::MONOTONE_CONVEX;
}

static redukti::IRInstrumentType parse_instrument_type(const std::string &s)
{
    if (s == "DEPOSIT")  return redukti::IRInstrumentType::INSTRUMENT_TYPE_DEPOSIT;
    if (s == "FRA")      return redukti::IRInstrumentType::INSTRUMENT_TYPE_FRA;
    if (s == "FUTURES")  return redukti::IRInstrumentType::INSTRUMENT_TYPE_FUTURES;
    if (s == "OIS")      return redukti::IRInstrumentType::INSTRUMENT_TYPE_OIS_SWAP;
    // Default to plain vanilla interest rate swap.
    return redukti::IRInstrumentType::INSTRUMENT_TYPE_INTEREST_RATE_SWAP;
}

// ---------------------------------------------------------------------------
// JNI lifecycle
// ---------------------------------------------------------------------------

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM * /*vm*/, void * /*reserved*/)
{
    GOOGLE_PROTOBUF_VERIFY_VERSION;
    g_processor = redukti::get_request_processor();
    return JNI_VERSION_1_8;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM * /*vm*/, void * /*reserved*/)
{
    g_processor.reset();
    google::protobuf::ShutdownProtobufLibrary();
}

// ---------------------------------------------------------------------------
// getVersion
// ---------------------------------------------------------------------------

JNIEXPORT jstring JNICALL
Java_com_example_demo_openredukti_OpenReduktiNative_getVersion(
    JNIEnv *env, jclass /*clazz*/)
{
    json j = {
        {"status",  "OK"},
        {"library", "OpenRedukti"},
        {"version", "0.2.x"},
        {"bridge",  "JNI 1.0"},
        {"build",   __DATE__ " " __TIME__}
    };
    return to_jstring(env, j.dump());
}

// ---------------------------------------------------------------------------
// bootstrapCurves
//
// Expected JSON input:
// {
//   "curveDate":       "2024-01-15",
//   "currency":        "USD",               // optional, default USD
//   "interpolatorType":"MONOTONE_CONVEX",   // optional
//   "instruments": [
//     { "maturityDate":"2024-04-15", "parRate":0.0535, "instrumentType":"DEPOSIT" },
//     { "maturityDate":"2025-01-15", "parRate":0.0545, "instrumentType":"SWAP"    }
//   ]
// }
// ---------------------------------------------------------------------------

JNIEXPORT jstring JNICALL
Java_com_example_demo_openredukti_OpenReduktiNative_bootstrapCurves(
    JNIEnv *env, jclass /*clazz*/, jstring jsonRequest)
{
    try {
        json req = json::parse(jstr_to_std(env, jsonRequest));

        redukti::BootstrapCurvesRequest bsReq;
        bsReq.set_business_date(parse_date(req.at("curveDate").get<std::string>()));

        // Build curve definition (one curve per call for simplicity)
        auto *curveDef = bsReq.add_curve_definitions();
        curveDef->set_id(1);
        curveDef->set_currency(parse_currency(req.value("currency", "USD")));
        curveDef->set_interpolator_type(
            parse_interpolator(req.value("interpolatorType", "MONOTONE_CONVEX")));
        curveDef->set_index_family(redukti::IndexFamily::LIBOR);
        curveDef->set_tenor(redukti::Tenor::TENOR_3M);

        // Par rate instruments
        for (const auto &inst : req.at("instruments")) {
            auto *pr = bsReq.add_par_rates();
            pr->set_maturity_date(parse_date(inst.at("maturityDate").get<std::string>()));
            pr->set_value(inst.at("parRate").get<double>());
            pr->set_instrument_type(
                parse_instrument_type(inst.value("instrumentType", "SWAP")));
        }

        redukti::BootstrapCurvesReply reply;
        redukti::StatusCode sc = g_processor->handle(bsReq, &reply);

        if (sc != redukti::StatusCode::kOk) {
            return error_json(env, "Bootstrap failed, status=" + std::to_string(static_cast<int>(sc)));
        }

        // Build response JSON
        json resp;
        resp["status"]    = "OK";
        resp["curveDate"] = req.at("curveDate");
        resp["currency"]  = req.value("currency", "USD");

        json curvesArr = json::array();
        for (int i = 0; i < reply.curves_size(); ++i) {
            const auto &zc = reply.curves(i);
            json cv;
            cv["id"] = zc.id();
            json pts = json::array();
            // maturities and values arrays are parallel
            for (int j = 0; j < zc.maturities_size(); ++j) {
                json pt;
                pt["maturityDate"]   = format_date(zc.maturities(j));
                pt["discountFactor"] = zc.values(j);
                pts.push_back(pt);
            }
            cv["points"] = pts;
            curvesArr.push_back(cv);
        }
        resp["curves"] = curvesArr;
        return to_jstring(env, resp.dump());

    } catch (const std::exception &e) {
        return error_json(env, std::string("bootstrapCurves: ") + e.what());
    } catch (...) {
        return error_json(env, "bootstrapCurves: unknown exception");
    }
}

// ---------------------------------------------------------------------------
// valueCashflows
//
// Expected JSON input:
// {
//   "tradeDate":        "2024-01-15",
//   "currency":         "USD",
//   "notional":         1000000.0,
//   "fixedRate":        0.055,
//   "payFixed":         true,
//   "effectiveDate":    "2024-01-17",
//   "terminationDate":  "2029-01-17",
//   "derivativeOrder":  1,              // 0=PV only, 1=delta, 2=gamma
//   "zeroCurve": {
//     "id": 1,
//     "points": [
//       { "maturityDate": "2024-04-17", "discountFactor": 0.9868 },
//       ...
//     ]
//   }
// }
// ---------------------------------------------------------------------------

JNIEXPORT jstring JNICALL
Java_com_example_demo_openredukti_OpenReduktiNative_valueCashflows(
    JNIEnv *env, jclass /*clazz*/, jstring jsonRequest)
{
    try {
        json req = json::parse(jstr_to_std(env, jsonRequest));

        redukti::ValuationRequest valReq;
        valReq.set_business_date(parse_date(req.at("tradeDate").get<std::string>()));
        valReq.set_derivative_order(req.value("derivativeOrder", 1));

        // Provide the zero curve so the library can price the cashflows
        if (req.contains("zeroCurve")) {
            const auto &zcJson = req.at("zeroCurve");
            auto *zc = valReq.add_zero_curves();
            zc->set_id(zcJson.value("id", 1));
            zc->set_currency(parse_currency(req.value("currency", "USD")));
            for (const auto &pt : zcJson.at("points")) {
                zc->add_maturities(parse_date(pt.at("maturityDate").get<std::string>()));
                zc->add_values(pt.at("discountFactor").get<double>());
            }
        }

        // Build a simple fixed-float IRS cashflow collection.
        // For a full schedule, the caller should pre-compute payment dates
        // or we can use OpenRedukti's schedule builder (see schedule.h).
        auto *cfColl = valReq.mutable_cashflows();

        // Fixed leg – single stream of CFSimple payments
        {
            auto *stream = cfColl->add_streams();
            stream->set_pay_currency(parse_currency(req.value("currency", "USD")));

            double notional  = req.value("notional",  1000000.0);
            double fixedRate = req.at("fixedRate").get<double>();
            bool   payFixed  = req.value("payFixed",  true);
            int32_t effDate  = parse_date(req.at("effectiveDate").get<std::string>());
            int32_t termDate = parse_date(req.at("terminationDate").get<std::string>());

            // One simplified annual fixed coupon per year
            // (A production wrapper would call OpenRedukti's schedule builder)
            int years = (termDate / 10000) - (effDate / 10000);
            for (int y = 1; y <= years; ++y) {
                auto *cf = stream->add_cashflows();
                auto *simple = cf->mutable_simple();
                simple->set_pay_date(effDate + y * 10000); // crude +1 year
                double coupon = notional * fixedRate;
                simple->set_notional(payFixed ? -coupon : coupon);
                simple->set_currency(parse_currency(req.value("currency", "USD")));
            }
        }

        redukti::ValuationReply reply;
        redukti::StatusCode sc = g_processor->handle(valReq, &reply);

        if (sc != redukti::StatusCode::kOk) {
            return error_json(env, "Valuation failed, status=" + std::to_string(static_cast<int>(sc)));
        }

        json resp;
        resp["status"]    = "OK";
        resp["tradeDate"] = req.at("tradeDate");
        resp["currency"]  = req.value("currency", "USD");

        // PV per scenario (usually one scenario for a simple request)
        json pvArr = json::array();
        for (int i = 0; i < reply.pv_size(); ++i) {
            pvArr.push_back(reply.pv(i));
        }
        resp["presentValues"] = pvArr;
        resp["presentValue"]  = reply.pv_size() > 0 ? reply.pv(0) : 0.0;

        // Delta (first-order sensitivity) if requested
        if (req.value("derivativeOrder", 1) >= 1 &&
            reply.has_sensitivities()) {
            json deltaArr = json::array();
            const auto &sens = reply.sensitivities();
            for (int i = 0; i < sens.pv_by_curve_size(); ++i) {
                const auto &curveEntry = sens.pv_by_curve(i);
                json entry;
                entry["curveId"] = curveEntry.curve_id();
                json deltas = json::array();
                for (int j = 0; j < curveEntry.delta_size(); ++j) {
                    deltas.push_back(curveEntry.delta(j));
                }
                entry["delta"] = deltas;
                deltaArr.push_back(entry);
            }
            resp["sensitivities"] = deltaArr;
        }

        return to_jstring(env, resp.dump());

    } catch (const std::exception &e) {
        return error_json(env, std::string("valueCashflows: ") + e.what());
    } catch (...) {
        return error_json(env, "valueCashflows: unknown exception");
    }
}

} // extern "C"

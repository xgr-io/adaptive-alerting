/*
 * Copyright 2018-2019 Expedia Group, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.expedia.adaptivealerting.anomdetect.detect.algo;

import com.expedia.adaptivealerting.anomdetect.detect.ForecastingOutlierDetector;
import com.expedia.adaptivealerting.anomdetect.detect.TypedDetectorBuilder;
import com.expedia.adaptivealerting.anomdetect.forecast.algo.ExponentialWelfordIntervalForecaster;
import com.expedia.adaptivealerting.anomdetect.forecast.algo.ExponentialWelfordIntervalForecasterParams;
import com.expedia.adaptivealerting.anomdetect.forecast.IntervalForecaster;
import lombok.val;

import java.util.Map;

/**
 * Abstract base class for implementing builders for legacy forecasting detectors.
 *
 * @deprecated This class supports the legacy treatment of point forecast methods are corresponding to anomaly
 * detectors. This assumption however is incorrect as we need to be able to create forecasting anomaly detectors by
 * mixing and matching point and interval forecast methods.
 */
@Deprecated
public abstract class LegacyForecastingOutlierDetectorBuilder
        implements TypedDetectorBuilder<ForecastingOutlierDetector> {

    protected IntervalForecaster toIntervalForecaster(Map<String, Object> paramsMap) {
        val alpha = (Double) paramsMap.get("alpha");
        val weakSigmas = (Double) paramsMap.get("weakSigmas");
        val strongSigmas = (Double) paramsMap.get("strongSigmas");
        val initVarianceEstimate = (Double) paramsMap.get("initVarianceEstimate");

        val params = new ExponentialWelfordIntervalForecasterParams();
        if (alpha != null) {
            params.setAlpha(alpha);
        }
        if (weakSigmas != null) {
            params.setWeakSigmas(weakSigmas);
        }
        if (strongSigmas != null) {
            params.setStrongSigmas(strongSigmas);
        }
        if (initVarianceEstimate != null) {
            params.setInitVarianceEstimate(initVarianceEstimate);
        }

        return new ExponentialWelfordIntervalForecaster(params);
    }
}

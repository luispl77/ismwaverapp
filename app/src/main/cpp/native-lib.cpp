#include <jni.h>
#include <string>
#include <vector>



bool isRecordingContinuous = false;

enum Mode {
    TRANSMIT,
    RECEIVE,
    TERMINAL
};

Mode currentMode = TERMINAL;  // Default mode
std::vector<char> dataBuffer;
std::vector<char> commandBuffer;


extern "C" {

JNIEXPORT jbyteArray JNICALL Java_com_emwaver_ismwaver_USBService_getDataBuffer(JNIEnv *env, jobject) {
    if (dataBuffer.empty()) {
        return nullptr;
    }

    jbyteArray javaArray = env->NewByteArray(dataBuffer.size());
    env->SetByteArrayRegion(javaArray, 0, dataBuffer.size(), reinterpret_cast<const jbyte*>(dataBuffer.data()));
    return javaArray;
}
JNIEXPORT void JNICALL Java_com_emwaver_ismwaver_USBService_loadDataBuffer(JNIEnv *env, jobject, jbyteArray data) {
    jsize dataSize = env->GetArrayLength(data);
    jbyte* dataBytes = env->GetByteArrayElements(data, 0);

    // Clear existing data in dataBuffer
    dataBuffer.clear();

    // Load new data
    for (int i = 0; i < dataSize; i++) {
        dataBuffer.push_back(static_cast<char>(dataBytes[i]));
    }

    env->ReleaseByteArrayElements(data, dataBytes, 0);
}
JNIEXPORT jboolean JNICALL Java_com_emwaver_ismwaver_USBService_getRecordingContinuous(JNIEnv *env, jobject) {
    return currentMode == RECEIVE;
}
JNIEXPORT void JNICALL Java_com_emwaver_ismwaver_USBService_setMode(JNIEnv *env, jobject, jint mode) {
    currentMode = Mode(mode);
}
JNIEXPORT void JNICALL Java_com_emwaver_ismwaver_USBService_sendIntentToTerminalNative(JNIEnv *env, jobject javaService, jbyteArray data) {
    jclass serviceClass = env->GetObjectClass(javaService);
    jmethodID sendIntentMethod = env->GetMethodID(serviceClass, "printStringBytes", "([B)V");

    env->CallVoidMethod(javaService, sendIntentMethod, data);
}
JNIEXPORT void JNICALL Java_com_emwaver_ismwaver_USBService_addToBuffer(JNIEnv *env, jobject serialService, jbyteArray data) {
    jbyte* bufferPtr = env->GetByteArrayElements(data, nullptr);
    jsize lengthOfArray = env->GetArrayLength(data);

    std::vector<char>& targetBuffer = (currentMode == RECEIVE) ? dataBuffer : commandBuffer;
    targetBuffer.insert(targetBuffer.end(), bufferPtr, bufferPtr + lengthOfArray);
    env->ReleaseByteArrayElements(data, bufferPtr, JNI_ABORT);

    if (currentMode == TERMINAL) {
        Java_com_emwaver_ismwaver_USBService_sendIntentToTerminalNative(env, serialService, data);
    }
}
JNIEXPORT jint JNICALL Java_com_emwaver_ismwaver_USBService_getCommandBufferLength(JNIEnv *env, jobject) {
    return static_cast<jint>(commandBuffer.size());
}
JNIEXPORT jint JNICALL Java_com_emwaver_ismwaver_USBService_getDataBufferLength(JNIEnv *env, jobject) {
    return static_cast<jint>(dataBuffer.size());
}
JNIEXPORT void JNICALL Java_com_emwaver_ismwaver_USBService_clearDataBuffer(JNIEnv *env, jobject) {
    dataBuffer.clear();
}
JNIEXPORT void JNICALL Java_com_emwaver_ismwaver_USBService_clearCommandBuffer(JNIEnv *env, jobject) {
    commandBuffer.clear();
}
JNIEXPORT jbyteArray JNICALL Java_com_emwaver_ismwaver_USBService_pollData(JNIEnv *env, jobject, jint length) {
    int lenToPoll = std::min(static_cast<int>(commandBuffer.size()), length);
    jbyteArray returnArray = env->NewByteArray(lenToPoll);

    if (lenToPoll > 0) {
    auto startIt = commandBuffer.begin();
    auto endIt = startIt + lenToPoll;

    // Copy the data into a temporary buffer
    std::vector<char> tempBuffer(startIt, endIt);
    env->SetByteArrayRegion(returnArray, 0, lenToPoll, reinterpret_cast<const jbyte*>(tempBuffer.data()));

    // Remove the polled data from the buffer
    commandBuffer.erase(startIt, endIt);
}

return returnArray;
}
JNIEXPORT jbyteArray JNICALL Java_com_emwaver_ismwaver_USBService_getBufferRange(JNIEnv *env, jobject, jint start, jint end) {
    int lenToCopy = end - start;
    if (lenToCopy <= 0 || start < 0 || start >= dataBuffer.size() || end > dataBuffer.size()) {
        return env->NewByteArray(0); // Return an empty array if parameters are invalid
    }

    jbyteArray returnArray = env->NewByteArray(lenToCopy);
    env->SetByteArrayRegion(returnArray, 0, lenToCopy, reinterpret_cast<const jbyte*>(&dataBuffer[start]));
    return returnArray;
}
JNIEXPORT jint JNICALL Java_com_emwaver_ismwaver_USBService_getStatusNumber(JNIEnv *env, jobject) {
    const std::string HEADER = "BS";
    const size_t HEADER_SIZE = HEADER.size();
    const size_t STATUS_SIZE = 2; // Assuming status number is 2 bytes

    // Search for the header from the end of the buffer
    for (size_t i = commandBuffer.size(); i >= HEADER_SIZE + STATUS_SIZE; --i) {
        std::string currentHeader(commandBuffer.begin() + i - HEADER_SIZE - STATUS_SIZE, commandBuffer.begin() + i - STATUS_SIZE);
        if (currentHeader == HEADER) {
            // Parse the status number
            uint16_t status = (static_cast<uint8_t>(commandBuffer[i - STATUS_SIZE]) << 8) | static_cast<uint8_t>(commandBuffer[i - STATUS_SIZE + 1]);

            // Clear the buffer from the end of the parsed packet to the end of the buffer
            commandBuffer.erase(commandBuffer.begin() + i, commandBuffer.end());

            return static_cast<jint>(status);
        }
    }

    // Return a default value if the correct packet is not found
    return -1;
}
//Line graph compression
JNIEXPORT jobjectArray JNICALL Java_com_emwaver_ismwaver_USBService_compressDataBits(JNIEnv *env, jobject, jint rangeStart, jint rangeEnd, jint numberBins) {

    float timePerSample = 1.0f; // todo: fix scale to have 10us per sample
    float totalPointsInRange = (rangeEnd - rangeStart)/timePerSample;
    std::vector<float> timeValues;
    std::vector<float> dataValues;

    jclass floatArrayClass = env->FindClass("[F");
    jobjectArray result = env->NewObjectArray(2, floatArrayClass, nullptr);

    if (totalPointsInRange <= numberBins*2) {
        // Less data than bins, simply convert each bit to a point
        for (int i = rangeStart; i < rangeEnd; ++i) {
            int byteIndex = i / 8;
            int bitIndex = i % 8;
            if (byteIndex < dataBuffer.size()) {
                uint8_t bit = (dataBuffer[byteIndex] >> bitIndex) & 1;
                timeValues.push_back(static_cast<float>(i * timePerSample));
                dataValues.push_back(bit ? 255.0f : 0.0f);
            }
        }
    } else {
        // More data than bins, compress using min-max in each bin
        float binWidth = totalPointsInRange / static_cast<float>(numberBins);
        for (int bin = 0; bin < numberBins; ++bin) {
            int binStart = static_cast<int>(rangeStart + bin * binWidth);
            int binEnd = static_cast<int>(binStart + binWidth);
            binEnd = std::min(binEnd, rangeEnd);

            bool foundData = false;
            float minVal = 255.0f;
            float maxVal = 0.0f;

            for (int i = binStart; i < binEnd; ++i) {
                int byteIndex = i / 8;
                int bitIndex = i % 8;
                if (byteIndex < dataBuffer.size()) {
                    uint8_t bit = (dataBuffer[byteIndex] >> bitIndex) & 1;
                    float value = bit ? 255.0f : 0.0f;
                    minVal = std::min(minVal, value);
                    maxVal = std::max(maxVal, value);
                    foundData = true;
                }
            }

            if (foundData) {
                // Store min and max as two points for the current bin
                timeValues.push_back(static_cast<float>(binStart * timePerSample));
                dataValues.push_back(minVal);
                timeValues.push_back(static_cast<float>((binEnd - 1) * timePerSample));
                dataValues.push_back(maxVal);
            }
        }
    }

    jfloatArray timeArray = env->NewFloatArray(timeValues.size());
    jfloatArray dataArray = env->NewFloatArray(dataValues.size());
    env->SetFloatArrayRegion(timeArray, 0, timeValues.size(), timeValues.data());
    env->SetFloatArrayRegion(dataArray, 0, dataValues.size(), dataValues.data());

    env->SetObjectArrayElement(result, 0, timeArray);
    env->SetObjectArrayElement(result, 1, dataArray);

    env->DeleteLocalRef(timeArray);
    env->DeleteLocalRef(dataArray);

    return result;
}
//Analysis
JNIEXPORT jlongArray JNICALL Java_com_emwaver_ismwaver_USBService_findHighEdges(JNIEnv *env, jobject, jint samplesPerSymbol, jint errorTolerance) {
    std::vector<jlong> edges;
    bool isHigh = false;
    jlong startOfPulse = 0;
    jlong highSampleCount = 0;
    jlong lowSampleCount = 0;

    auto isPulseLengthValid = [samplesPerSymbol, errorTolerance](jlong length) {
        for (int n = 1; n * samplesPerSymbol <= length + errorTolerance; ++n) {
            if (std::abs(length - n * samplesPerSymbol) <= errorTolerance) {
                return true;
            }
        }
        return false;
    };

    for (jlong byteIndex = 0; byteIndex < dataBuffer.size(); ++byteIndex) {
        char currentByte = dataBuffer[byteIndex];
        for (int bitIndex = 0; bitIndex <= 7; bitIndex++) {
            bool currentState = (currentByte & (1 << bitIndex)) != 0;

            if (!isHigh && currentState) {
                // Transition from LOW to HIGH
                isHigh = true;
                startOfPulse = byteIndex * 8 + bitIndex;
                highSampleCount = 1;
                lowSampleCount = 0;
            } else if (isHigh) {
                if (currentState) {
                    // Continue counting HIGH samples, adding LOW samples if within error tolerance
                    if (lowSampleCount <= errorTolerance) {
                        highSampleCount += lowSampleCount + 1;
                        lowSampleCount = 0;
                    } else {
                        // Check if the collected pulse length is valid before starting a new count
                        if (isPulseLengthValid(highSampleCount)) {
                            edges.push_back(startOfPulse); // Start of previous valid pulse
                            edges.push_back(byteIndex * 8 + bitIndex - lowSampleCount - 1); // End of previous valid pulse
                        }
                        // Start a new pulse count
                        startOfPulse = byteIndex * 8 + bitIndex;
                        highSampleCount = 1;
                        lowSampleCount = 0;
                    }
                } else {
                    // Increment LOW sample count within a HIGH pulse
                    lowSampleCount++;
                }
            }
        }
    }

    // Check for a trailing HIGH pulse at the end of data
    if (isHigh && isPulseLengthValid(highSampleCount)) {
        edges.push_back(startOfPulse); // Start of pulse
        edges.push_back(dataBuffer.size() * 8 - 1); // End of pulse
    }

    jlongArray result = env->NewLongArray(edges.size());
    env->SetLongArrayRegion(result, 0, edges.size(), edges.data());

    return result;
}
JNIEXPORT jbyteArray JNICALL Java_com_emwaver_ismwaver_USBService_extractBitsFromEdges(JNIEnv *env, jobject, jlongArray edgeArray, jint samplesPerSymbol) {
    jsize edgeCount = env->GetArrayLength(edgeArray);
    jlong *edges = env->GetLongArrayElements(edgeArray, NULL);

    std::vector<jbyte> bitStream;
    jbyte currentByte = 0;
    int bitPosition = 0;

    for (jsize i = 0; i < edgeCount - 1; i++) {
        jlong pulseLength = edges[i + 1] - edges[i];
        bool isHigh = i % 2 == 0; // Even index: HIGH pulse, Odd index: LOW pulse
        int numBits = static_cast<int>((pulseLength + samplesPerSymbol / 2) / samplesPerSymbol);

        for (int j = 0; j < numBits; j++) {
            currentByte |= (isHigh ? 1 : 0) << (7 - bitPosition); // Set bit for HIGH pulse
            bitPosition++;

            if (bitPosition == 8) {
                bitStream.push_back(currentByte);
                currentByte = 0;
                bitPosition = 0;
            }
        }
    }

    if (bitPosition > 0) {
        bitStream.push_back(currentByte);
    }

    env->ReleaseLongArrayElements(edgeArray, edges, JNI_ABORT);

    jbyteArray byteArray = env->NewByteArray(bitStream.size());
    env->SetByteArrayRegion(byteArray, 0, bitStream.size(), &bitStream[0]);

    return byteArray;
}



}
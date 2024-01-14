#include <jni.h>
#include <string>
#include <vector>

std::vector<char> dataBuffer;

bool isRecordingContinuous = false;

extern "C" {
// Define constants for HIGH and LOW states
const char HIGH = 124;
const char LOW = 48;

JNIEXPORT jlongArray JNICALL Java_com_emwaver_ismwaver_SerialService_findPulseEdges(
        JNIEnv *env, jobject, jint samplesPerSymbol, jint errorTolerance, jint maxLowPulseMultiplier) {

    std::vector<jlong> edges;
    char lastState = dataBuffer[0];
    jlong edgePosition = 0;
    jlong lastEdgePosition = 0;

    for (jlong i = 0; i < dataBuffer.size(); ++i) {
        if (dataBuffer[i] != lastState) {
            edgePosition = i;
            jlong pulseLength = edgePosition - lastEdgePosition;

            bool validPulse = false;
            if (lastState == HIGH) {
                jlong diff = std::abs(pulseLength - samplesPerSymbol);
                validPulse = (diff <= errorTolerance || pulseLength % samplesPerSymbol <= errorTolerance);
            } else if (lastState == LOW) {
                validPulse = (pulseLength >= samplesPerSymbol &&
                              pulseLength <= samplesPerSymbol * maxLowPulseMultiplier);
            }

            if (validPulse) {
                edges.push_back(edgePosition);
                lastState = dataBuffer[i];
            }
            lastEdgePosition = edgePosition;
        }
    }

    // Convert std::vector<jlong> to jlongArray for return
    jlongArray result = env->NewLongArray(edges.size());
    env->SetLongArrayRegion(result, 0, edges.size(), edges.data());

    return result;
}

JNIEXPORT void JNICALL Java_com_emwaver_ismwaver_SerialService_setRecordingContinuous(JNIEnv *env, jobject, jboolean recording) {
    isRecordingContinuous = recording;
}

JNIEXPORT jboolean JNICALL Java_com_emwaver_ismwaver_SerialService_getRecordingContinuous(JNIEnv *env, jobject) {
    return isRecordingContinuous;
}

JNIEXPORT void JNICALL Java_com_emwaver_ismwaver_SerialService_sendIntentToTerminalNative(JNIEnv *env, jobject javaService, jbyteArray data) {
    jclass serviceClass = env->GetObjectClass(javaService);
    jmethodID sendIntentMethod = env->GetMethodID(serviceClass, "sendIntentToTerminal", "([B)V");

    env->CallVoidMethod(javaService, sendIntentMethod, data);
}

JNIEXPORT void JNICALL Java_com_emwaver_ismwaver_SerialService_addToBuffer(JNIEnv *env, jobject serialService, jbyteArray data) {

    jbyte* bufferPtr = env->GetByteArrayElements(data, nullptr);
    jsize lengthOfArray = env->GetArrayLength(data);

    dataBuffer.insert(dataBuffer.end(), bufferPtr, bufferPtr + lengthOfArray);
    env->ReleaseByteArrayElements(data, bufferPtr, JNI_ABORT);

    if (!isRecordingContinuous) {
        Java_com_emwaver_ismwaver_SerialService_sendIntentToTerminalNative(env, serialService, data);
    }
}

JNIEXPORT jint JNICALL Java_com_emwaver_ismwaver_SerialService_getBufferLength(JNIEnv *env, jobject) {
    return static_cast<jint>(dataBuffer.size());
}

JNIEXPORT void JNICALL Java_com_emwaver_ismwaver_SerialService_clearBuffer(JNIEnv *env, jobject) {
    dataBuffer.clear();
}


JNIEXPORT jbyteArray JNICALL Java_com_emwaver_ismwaver_SerialService_pollData(JNIEnv *env, jobject, jint length) {
int lenToPoll = std::min(static_cast<int>(dataBuffer.size()), length);
jbyteArray returnArray = env->NewByteArray(lenToPoll);

if (lenToPoll > 0) {
auto startIt = dataBuffer.begin();
auto endIt = startIt + lenToPoll;

// Copy the data into a temporary buffer
std::vector<char> tempBuffer(startIt, endIt);
env->SetByteArrayRegion(returnArray, 0, lenToPoll, reinterpret_cast<const jbyte*>(tempBuffer.data()));

// Remove the polled data from the buffer
dataBuffer.erase(startIt, endIt);
}

return returnArray;
}

JNIEXPORT jint JNICALL Java_com_emwaver_ismwaver_SerialService_getBufferStatus(JNIEnv *env, jobject) {
    const int PACKET_SIZE = 64;
    const int HEADER_SIZE = 2;
    const std::string HEADER = "BS";

    if (dataBuffer.size() >= PACKET_SIZE) {
        // Get the last 64 bytes from the buffer
        std::vector<char> lastPacket(dataBuffer.end() - PACKET_SIZE, dataBuffer.end());

        // Check for the header
        std::string packetHeader(lastPacket.begin(), lastPacket.begin() + HEADER_SIZE);
        if (packetHeader == HEADER) {
            // Parse the container size
            uint16_t containerSize = (static_cast<uint8_t>(lastPacket[2]) << 8) | static_cast<uint8_t>(lastPacket[3]);
            if(containerSize < 1000)
                return static_cast<jint>(containerSize);
        }
    }

    // Return a default value if the correct packet is not found
    return -1;
}





JNIEXPORT jobjectArray JNICALL Java_com_emwaver_ismwaver_SerialService_compressDataBits(JNIEnv *env, jobject, jint rangeStart, jint rangeEnd, jint numberBins) {
    rangeStart *= 8; // Convert byte range to bit range
    rangeEnd *= 8;
    float totalPointsInRange = rangeEnd - rangeStart;
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
                timeValues.push_back(static_cast<float>(i / 8.0f));
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
                timeValues.push_back(static_cast<float>(binStart / 8.0f));
                dataValues.push_back(minVal);
                timeValues.push_back(static_cast<float>((binEnd - 1) / 8.0f));
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




}
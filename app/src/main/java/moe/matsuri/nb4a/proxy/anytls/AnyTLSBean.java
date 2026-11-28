package moe.matsuri.nb4a.proxy.anytls;

import androidx.annotation.NonNull;

import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;

import org.jetbrains.annotations.NotNull;

import io.nekohasekai.sagernet.fmt.AbstractBean;
import io.nekohasekai.sagernet.fmt.KryoConverters;

public class AnyTLSBean extends AbstractBean {

    public static final Creator<AnyTLSBean> CREATOR = new CREATOR<AnyTLSBean>() {
        @NonNull
        @Override
        public AnyTLSBean newInstance() {
            return new AnyTLSBean();
        }

        @Override
        public AnyTLSBean[] newArray(int size) {
            return new AnyTLSBean[size];
        }
    };
    public String password;
    public String sni;
    public String alpn;
    public String certificates;
    public String utlsFingerprint;
    public Boolean allowInsecure;
    // In sing-box, this seemed can be used with REALITY.
    // But even mihomo appended many options, it still not provide REALITY.
    // https://github.com/anytls/anytls-go/blob/4636d90462fa21a510420512d7706a9acf69c7b9/docs/faq.md?plain=1#L25-L37

    public String echConfig;
    public String realityPubKey;
    public String realityShortId;
    public String idleSessionCheckInterval;
    public String idleSessionTimeout;
    public Integer minIdleSession;
    public Boolean disableSNI;
    public String clientCert;
    public String clientKey;
    public String certPublicKeySha256;
    public String certificateSha256;
    public String echQueryServerName;
    public Boolean tlsFragment;
    public String tlsFragmentFallbackDelay;
    public Boolean tlsRecordFragment;
    public String tlsSpoof;
    public String tlsSpoofMethod;

    @Override
    public void initializeDefaultValues() {
        super.initializeDefaultValues();
        if (password == null) password = "";
        if (sni == null) sni = "";
        if (alpn == null) alpn = "";
        if (certificates == null) certificates = "";
        if (utlsFingerprint == null) utlsFingerprint = "";
        if (allowInsecure == null) allowInsecure = false;
        if (echConfig == null) echConfig = "";
        if (realityPubKey == null) realityPubKey = "";
        if (realityShortId == null) realityShortId = "";
        if (idleSessionCheckInterval == null) idleSessionCheckInterval = "";
        if (idleSessionTimeout == null) idleSessionTimeout = "";
        if (minIdleSession == null) minIdleSession = 0;
        if (disableSNI == null) disableSNI = false;
        if (clientCert == null) clientCert = "";
        if (clientKey == null) clientKey = "";
        if (certPublicKeySha256 == null) certPublicKeySha256 = "";
        if (certificateSha256 == null) certificateSha256 = "";
        if (echQueryServerName == null) echQueryServerName = "";
        if (tlsFragment == null) tlsFragment = false;
        if (tlsFragmentFallbackDelay == null) tlsFragmentFallbackDelay = "";
        if (tlsRecordFragment == null) tlsRecordFragment = false;
        if (tlsSpoof == null) tlsSpoof = "";
        if (tlsSpoofMethod == null) tlsSpoofMethod = "";
    }

    @Override
    public void serialize(ByteBufferOutput output) {
        output.writeInt(2);
        super.serialize(output);
        output.writeString(password);
        output.writeString(sni);
        output.writeString(alpn);
        output.writeString(certificates);
        output.writeString(utlsFingerprint);
        output.writeBoolean(allowInsecure);
        output.writeString(echConfig);
        output.writeString(realityPubKey);
        output.writeString(realityShortId);
        // v2 (husi-dev parity)
        output.writeString(idleSessionCheckInterval);
        output.writeString(idleSessionTimeout);
        output.writeInt(minIdleSession);
        output.writeBoolean(disableSNI);
        output.writeString(clientCert);
        output.writeString(clientKey);
        output.writeString(certPublicKeySha256);
        output.writeString(certificateSha256);
        output.writeString(echQueryServerName);
        output.writeBoolean(tlsFragment);
        output.writeString(tlsFragmentFallbackDelay);
        output.writeBoolean(tlsRecordFragment);
        output.writeString(tlsSpoof);
        output.writeString(tlsSpoofMethod);
    }

    @Override
    public void deserialize(ByteBufferInput input) {
        int version = input.readInt();
        super.deserialize(input);
        password = input.readString();
        sni = input.readString();
        alpn = input.readString();
        certificates = input.readString();
        utlsFingerprint = input.readString();
        allowInsecure = input.readBoolean();
        echConfig = input.readString();
        if (version >= 1) {
            realityPubKey = input.readString();
            realityShortId = input.readString();
        } else {
            realityPubKey = "";
            realityShortId = "";
        }
        if (version >= 2) {
            idleSessionCheckInterval = input.readString();
            idleSessionTimeout = input.readString();
            minIdleSession = input.readInt();
            disableSNI = input.readBoolean();
            clientCert = input.readString();
            clientKey = input.readString();
            certPublicKeySha256 = input.readString();
            certificateSha256 = input.readString();
            echQueryServerName = input.readString();
            tlsFragment = input.readBoolean();
            tlsFragmentFallbackDelay = input.readString();
            tlsRecordFragment = input.readBoolean();
            tlsSpoof = input.readString();
            tlsSpoofMethod = input.readString();
        }
    }

    @NotNull
    @Override
    public AnyTLSBean clone() {
        return KryoConverters.deserialize(new AnyTLSBean(), KryoConverters.serialize(this));
    }
}

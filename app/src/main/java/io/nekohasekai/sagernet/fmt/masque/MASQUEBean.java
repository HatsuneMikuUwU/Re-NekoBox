package io.nekohasekai.sagernet.fmt.masque;

import androidx.annotation.NonNull;

import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;

import org.jetbrains.annotations.NotNull;

import io.nekohasekai.sagernet.fmt.AbstractBean;
import io.nekohasekai.sagernet.fmt.KryoConverters;

public class MASQUEBean extends AbstractBean {

    public String username;
    public String password;
    public String path;
    public Integer httpVersion;
    public Boolean disableVersionFallback;
    public Integer mtu;
    public Boolean enableTLS;
    public String sni;
    public Boolean allowInsecure;
    public Boolean disableSNI;
    public String alpn;
    public String certificates;
    public String utlsFingerprint;
    public String echConfig;

    @Override
    public void initializeDefaultValues() {
        super.initializeDefaultValues();
        if (serverPort == null || serverPort == 0) serverPort = 443;
        if (username == null) username = "";
        if (password == null) password = "";
        if (path == null) path = "";
        if (httpVersion == null) httpVersion = 3;
        if (disableVersionFallback == null) disableVersionFallback = false;
        if (mtu == null || mtu == 0) mtu = 1280;
        if (enableTLS == null) enableTLS = true;
        if (sni == null) sni = "";
        if (allowInsecure == null) allowInsecure = false;
        if (disableSNI == null) disableSNI = false;
        if (alpn == null) alpn = "";
        if (certificates == null) certificates = "";
        if (utlsFingerprint == null) utlsFingerprint = "";
        if (echConfig == null) echConfig = "";
    }

    @Override
    public void serialize(ByteBufferOutput output) {
        output.writeInt(0);
        super.serialize(output);
        output.writeString(username);
        output.writeString(password);
        output.writeString(path);
        output.writeInt(httpVersion);
        output.writeBoolean(disableVersionFallback);
        output.writeInt(mtu);
        output.writeBoolean(enableTLS);
        output.writeString(sni);
        output.writeBoolean(allowInsecure);
        output.writeBoolean(disableSNI);
        output.writeString(alpn);
        output.writeString(certificates);
        output.writeString(utlsFingerprint);
        output.writeString(echConfig);
    }

    @Override
    public void deserialize(ByteBufferInput input) {
        int version = input.readInt();
        super.deserialize(input);
        username = input.readString();
        password = input.readString();
        path = input.readString();
        httpVersion = input.readInt();
        disableVersionFallback = input.readBoolean();
        mtu = input.readInt();
        enableTLS = input.readBoolean();
        sni = input.readString();
        allowInsecure = input.readBoolean();
        disableSNI = input.readBoolean();
        alpn = input.readString();
        certificates = input.readString();
        utlsFingerprint = input.readString();
        echConfig = input.readString();
    }

    @NotNull
    @Override
    public MASQUEBean clone() {
        return KryoConverters.deserialize(new MASQUEBean(), KryoConverters.serialize(this));
    }

    public static final Creator<MASQUEBean> CREATOR = new CREATOR<MASQUEBean>() {
        @NonNull
        @Override
        public MASQUEBean newInstance() {
            return new MASQUEBean();
        }

        @Override
        public MASQUEBean[] newArray(int size) {
            return new MASQUEBean[size];
        }
    };
}

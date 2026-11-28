package io.nekohasekai.sagernet.fmt.openvpn;

import androidx.annotation.NonNull;

import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;

import org.jetbrains.annotations.NotNull;

import io.nekohasekai.sagernet.fmt.AbstractBean;
import io.nekohasekai.sagernet.fmt.KryoConverters;

public class OpenVPNBean extends AbstractBean {

    public String network;
    public String username;
    public String password;
    public String cipher;
    public String dataCiphers;
    public String auth;
    public String compression;
    public Boolean redirectGateway;
    public Integer mtu;
    public String serverName;
    public String certificate;
    public String clientCertificate;
    public String clientKey;
    public String peerFingerprint;
    public String controlWrapType;
    public String controlWrapKey;
    public String controlWrapDirection;

    @Override
    public void initializeDefaultValues() {
        super.initializeDefaultValues();
        if (serverPort == null || serverPort == 0) serverPort = 1194;
        if (network == null || network.isEmpty()) network = "udp";
        if (username == null) username = "";
        if (password == null) password = "";
        if (cipher == null) cipher = "";
        if (dataCiphers == null) dataCiphers = "";
        if (auth == null) auth = "";
        if (compression == null) compression = "";
        if (redirectGateway == null) redirectGateway = false;
        if (mtu == null || mtu == 0) mtu = 1500;
        if (serverName == null) serverName = "";
        if (certificate == null) certificate = "";
        if (clientCertificate == null) clientCertificate = "";
        if (clientKey == null) clientKey = "";
        if (peerFingerprint == null) peerFingerprint = "";
        if (controlWrapType == null) controlWrapType = "";
        if (controlWrapKey == null) controlWrapKey = "";
        if (controlWrapDirection == null) controlWrapDirection = "";
    }

    @Override
    public void serialize(ByteBufferOutput output) {
        output.writeInt(0);
        super.serialize(output);
        output.writeString(network);
        output.writeString(username);
        output.writeString(password);
        output.writeString(cipher);
        output.writeString(dataCiphers);
        output.writeString(auth);
        output.writeString(compression);
        output.writeBoolean(redirectGateway);
        output.writeInt(mtu);
        output.writeString(serverName);
        output.writeString(certificate);
        output.writeString(clientCertificate);
        output.writeString(clientKey);
        output.writeString(peerFingerprint);
        output.writeString(controlWrapType);
        output.writeString(controlWrapKey);
        output.writeString(controlWrapDirection);
    }

    @Override
    public void deserialize(ByteBufferInput input) {
        int version = input.readInt();
        super.deserialize(input);
        network = input.readString();
        username = input.readString();
        password = input.readString();
        cipher = input.readString();
        dataCiphers = input.readString();
        auth = input.readString();
        compression = input.readString();
        redirectGateway = input.readBoolean();
        mtu = input.readInt();
        serverName = input.readString();
        certificate = input.readString();
        clientCertificate = input.readString();
        clientKey = input.readString();
        peerFingerprint = input.readString();
        controlWrapType = input.readString();
        controlWrapKey = input.readString();
        controlWrapDirection = input.readString();
    }

    @NotNull
    @Override
    public OpenVPNBean clone() {
        return KryoConverters.deserialize(new OpenVPNBean(), KryoConverters.serialize(this));
    }

    public static final Creator<OpenVPNBean> CREATOR = new CREATOR<OpenVPNBean>() {
        @NonNull
        @Override
        public OpenVPNBean newInstance() {
            return new OpenVPNBean();
        }

        @Override
        public OpenVPNBean[] newArray(int size) {
            return new OpenVPNBean[size];
        }
    };
}

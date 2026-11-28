package io.nekohasekai.sagernet.fmt.openconnect;

import androidx.annotation.NonNull;

import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;

import org.jetbrains.annotations.NotNull;

import io.nekohasekai.sagernet.fmt.AbstractBean;
import io.nekohasekai.sagernet.fmt.KryoConverters;

public class OpenConnectBean extends AbstractBean {

    public String server;
    public String flavor;
    public String username;
    public String password;
    public String authGroup;
    public String reportedOS;
    public String userAgent;
    public Boolean allowInsecureCrypto;
    public Boolean tlsInsecure;
    public String tlsServerName;
    public String tlsPeerFingerprint;
    public String certificateAuthority;
    public String clientCertificate;
    public String clientKey;
    public String clientKeyPassword;

    @Override
    public void initializeDefaultValues() {
        super.initializeDefaultValues();
        if (serverPort == null || serverPort == 0) serverPort = 443;
        if (server == null) server = "";
        if (flavor == null) flavor = "";
        if (username == null) username = "";
        if (password == null) password = "";
        if (authGroup == null) authGroup = "";
        if (reportedOS == null) reportedOS = "";
        if (userAgent == null) userAgent = "";
        if (allowInsecureCrypto == null) allowInsecureCrypto = false;
        if (tlsInsecure == null) tlsInsecure = false;
        if (tlsServerName == null) tlsServerName = "";
        if (tlsPeerFingerprint == null) tlsPeerFingerprint = "";
        if (certificateAuthority == null) certificateAuthority = "";
        if (clientCertificate == null) clientCertificate = "";
        if (clientKey == null) clientKey = "";
        if (clientKeyPassword == null) clientKeyPassword = "";
    }

    @Override
    public void serialize(ByteBufferOutput output) {
        output.writeInt(0);
        super.serialize(output);
        output.writeString(server);
        output.writeString(flavor);
        output.writeString(username);
        output.writeString(password);
        output.writeString(authGroup);
        output.writeString(reportedOS);
        output.writeString(userAgent);
        output.writeBoolean(allowInsecureCrypto);
        output.writeBoolean(tlsInsecure);
        output.writeString(tlsServerName);
        output.writeString(tlsPeerFingerprint);
        output.writeString(certificateAuthority);
        output.writeString(clientCertificate);
        output.writeString(clientKey);
        output.writeString(clientKeyPassword);
    }

    @Override
    public void deserialize(ByteBufferInput input) {
        int version = input.readInt();
        super.deserialize(input);
        server = input.readString();
        flavor = input.readString();
        username = input.readString();
        password = input.readString();
        authGroup = input.readString();
        reportedOS = input.readString();
        userAgent = input.readString();
        allowInsecureCrypto = input.readBoolean();
        tlsInsecure = input.readBoolean();
        tlsServerName = input.readString();
        tlsPeerFingerprint = input.readString();
        certificateAuthority = input.readString();
        clientCertificate = input.readString();
        clientKey = input.readString();
        clientKeyPassword = input.readString();
    }

    @Override
    public String displayAddress() {
        if (server != null && !server.isEmpty()) return server;
        return super.displayAddress();
    }

    @NotNull
    @Override
    public OpenConnectBean clone() {
        return KryoConverters.deserialize(new OpenConnectBean(), KryoConverters.serialize(this));
    }

    public static final Creator<OpenConnectBean> CREATOR = new CREATOR<OpenConnectBean>() {
        @NonNull
        @Override
        public OpenConnectBean newInstance() {
            return new OpenConnectBean();
        }

        @Override
        public OpenConnectBean[] newArray(int size) {
            return new OpenConnectBean[size];
        }
    };
}

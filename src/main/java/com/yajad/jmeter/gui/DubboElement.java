package com.yajad.jmeter.gui;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.yajad.jmeter.dto.DubboParamDto;
import com.yajad.jmeter.parse.YamlParamParser;
import com.yajad.jmeter.util.JsonUtils;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.AbstractTestElement;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

public class DubboElement implements Serializable {
    static final String REGISTRY_PROTOCOL = "Yajad.registryProtocol";
    static final String REGISTRY_ADDRESS = "Yajad.registryAddress";
    static final String REGISTRY_GROUP = "Yajad.registryGroup";
    static final String RPC_PROTOCOL = "Yajad.rpcProtocol";
    static final String SERVICE_TIMEOUT = "Yajad.serviceTimeout";
    static final String SERVICE_RETRIES = "Yajad.serviceRetries";
    static final String SERVICE_VERSION = "Yajad.serviceVersion";
    static final String SERVICE_CLUSTER = "Yajad.serviceCluster";
    static final String SERVICE_GROUP = "Yajad.serviceGroup";
    static final String SERVICE_CONNECTIONS = "Yajad.serviceConnections";
    static final String SERVICE_LOAD_BALANCE = "Yajad.serviceLoadBalance";
    static final String SERVICE_INTERFACE = "Yajad.serviceInterface";
    static final String SERVICE_METHOD = "Yajad.serviceMethod";
    static final String SERVICE_PARAMETER = "Yajad.serviceParameter";
    private AbstractTestElement model;

    public DubboElement(AbstractTestElement model) {
        this.model = model;
    }

    public SampleResult sample() {
        SampleResult sampleResult = new SampleResult();

        sampleResult.setSampleLabel(model.getName());
        dubboInvoke(sampleResult);
        sampleResult.setDataType(SampleResult.TEXT);
        sampleResult.setResponseCodeOK();
        sampleResult.setResponseMessageOK();
        sampleResult.sampleEnd();

        return sampleResult;
    }

    @SuppressWarnings("deprecation")
    private void dubboInvoke(SampleResult sampleResult) {
        DubboParamDto parameters = YamlParamParser.parseParameter(getServiceParameter());

        ApplicationConfig application = new ApplicationConfig();
        application.setName("Yajad-Dubbo-Sample");

        ReferenceConfig reference = new ReferenceConfig();
        reference.setApplication(application);

        if ("none".equalsIgnoreCase(getRegistryProtocol())) {
            reference.setUrl(getRpcProtocol() + "://" + getRegistryAddress() + "/" + getServiceInterface());
        } else {
            RegistryConfig registry = new RegistryConfig();
            registry.setProtocol(getRegistryProtocol());
            registry.setGroup(getRegistryGroup());
            registry.setAddress(getRegistryAddress());
            reference.setRegistry(registry);
        }

        reference.setProtocol(getRpcProtocol());
        reference.setTimeout(getServiceTimeout());
        reference.setRetries(getServiceRetries());
        reference.setVersion(getServiceVersion());
        reference.setCluster(getServiceCluster());
        reference.setGroup(getServiceGroup());
        reference.setConnections(getServiceConnections());
        reference.setLoadbalance(getServiceLoadBalance());

        reference.setGeneric(true);
        reference.setInterface(getServiceInterface());

        Object genericService = reference.get();

        String[] parameterTypes = parameters.getTypes().toArray(new String[]{});
        Object[] parameterValues = parameters.getValues().toArray();

        String samplerData = getSampleData();
        samplerData += "parameterTypes: " + Arrays.toString(parameterTypes) + "\n";
        samplerData += "parameterValues: " + Arrays.toString(parameterValues) + "\n";
        samplerData += "parameterClasses: " + Arrays.stream(parameterValues)
                .map(value -> value.getClass().getName())
                .collect(Collectors.toList());
        sampleResult.setSamplerData(samplerData);

        sampleResult.sampleStart();
        Object result;
        try {
            Method method = genericService.getClass().getMethod("$invoke", String.class, String[].class, Object[].class);
            // 以下写法是兼容dubbo版本
            // 如果写成
            // genericService.$invoke(getServiceMethod(), parameterTypes, parameterValues);
            // 会报错：
            // java.lang.ClassCastException: org.apache.dubbo.common.bytecode.proxy0 cannot be cast to com.alibaba.dubbo.rpc.service.GenericService
            // dubbo 2.7.X虽然做了兼容处理，但生成的genericService是新版本的
            // 参见 org.apache.dubbo.config.ReferenceConfig 中的代码：
            // if (ProtocolUtils.isGeneric(this.getGeneric())) {
            //   this.interfaceClass = GenericService.class;
            // }
            result = method.invoke(genericService, getServiceMethod(), parameterTypes, parameterValues);
            sampleResult.setSuccessful(true);
        } catch (Exception e) {
            sampleResult.setSuccessful(true);
            result = e;
        }
        sampleResult.setResponseData(JsonUtils.toJson(result), StandardCharsets.UTF_8.name());
    }

    private String getSampleData() {
        return "Registry Protocol: " + getRegistryProtocol() + "\n" +
                "Registry Address: " + getRegistryAddress() + "\n" +
                "Registry Group: " + getRegistryGroup() + "\n" +
                "RPC Protocol: " + getRpcProtocol() + "\n" +
                "Service Timeout: " + getServiceTimeout() + "\n" +
                "Service Retries: " + getServiceRetries() + "\n" +
                "Service Version: " + getServiceVersion() + "\n" +
                "Service Cluster: " + getServiceCluster() + "\n" +
                "Service Group: " + getServiceGroup() + "\n" +
                "Service Connections: " + getServiceConnections() + "\n" +
                "Service Load Balance: " + getServiceLoadBalance() + "\n" +
                "Interface: " + getServiceInterface() + "\n" +
                "Method: " + getServiceMethod() + "\n" +
                "Parameter: \n" + getServiceParameter() + "\n";
    }

    private String getRegistryProtocol() {
        return getPropertyAsString(REGISTRY_PROTOCOL);
    }

    private String getRegistryAddress() {
        return getPropertyAsString(REGISTRY_ADDRESS, "zookeeper");
    }

    private String getRegistryGroup() {
        return getPropertyAsString(REGISTRY_GROUP, "");
    }

    private String getRpcProtocol() {
        return getPropertyAsString(RPC_PROTOCOL, "dubbo");
    }

    private int getServiceTimeout() {
        return getPropertyAsInteger(SERVICE_TIMEOUT, 120000);
    }

    private int getServiceRetries() {
        return getPropertyAsInteger(SERVICE_RETRIES, 0);
    }

    private String getServiceVersion() {
        return getPropertyAsString(SERVICE_VERSION, "");
    }

    private String getServiceCluster() {
        return getPropertyAsString(SERVICE_CLUSTER, "failfast");
    }

    private String getServiceGroup() {
        return getPropertyAsString(SERVICE_GROUP, "");
    }

    private Integer getServiceConnections() {
        return getPropertyAsInteger(SERVICE_CONNECTIONS, 100);
    }

    private String getServiceLoadBalance() {
        return getPropertyAsString(SERVICE_LOAD_BALANCE, "random");
    }

    private String getServiceInterface() {
        return getPropertyAsString(SERVICE_INTERFACE);
    }

    private String getServiceMethod() {
        return getPropertyAsString(SERVICE_METHOD);
    }

    private String getServiceParameter() {
        return getPropertyAsString(SERVICE_PARAMETER);
    }

    private String getPropertyAsString(String key) {
        return model.getPropertyAsString(key);
    }

    private String getPropertyAsString(String key, String defaultValue) {
        return model.getPropertyAsString(key, defaultValue);
    }

    private Integer getPropertyAsInteger(String key, Integer defaultValue) {
        Integer value = defaultValue;
        try {
            value = Integer.valueOf(model.getPropertyAsString(key));
        } catch (NumberFormatException ignored) {
        }
        return value;
    }
}

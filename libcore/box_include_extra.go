package libcore

import (
	"github.com/sagernet/sing-box/adapter/endpoint"
	"github.com/sagernet/sing-box/adapter/outbound"
	C "github.com/sagernet/sing-box/constant"
	"github.com/sagernet/sing-box/dns"
	"github.com/sagernet/sing-box/option"
	"github.com/sagernet/sing-box/protocol/masque"
	"github.com/sagernet/sing-box/protocol/openconnect"
	"github.com/sagernet/sing-box/protocol/openvpn"
	"github.com/sagernet/sing-box/protocol/tailscale"
)

func registerPreviewOutbounds(registry *outbound.Registry) {
	tailscale.RegisterTailcatOutbound(registry)
}

func registerPreviewEndpoints(registry *endpoint.Registry) {
	masque.RegisterEndpoint(registry)
	tailscale.RegisterEndpoint(registry)
	endpoint.Register[option.OpenVPNClientEndpointOptions](registry, C.TypeOpenVPNClient, openvpn.NewClientEndpoint)
	openconnect.RegisterEndpoint(registry)
}

func registerPreviewDNSTransports(registry *dns.TransportRegistry) {
	tailscale.RegistryTransport(registry)
	openvpn.RegisterDNSTransport(registry)
	openconnect.RegisterDNSTransport(registry)
}

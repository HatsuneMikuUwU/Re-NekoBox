//go:build !core_preview

package libcore

import (
	"github.com/sagernet/sing-box/adapter/endpoint"
	"github.com/sagernet/sing-box/adapter/outbound"
	"github.com/sagernet/sing-box/dns"
)

func registerPreviewOutbounds(registry *outbound.Registry) {
}

func registerPreviewEndpoints(registry *endpoint.Registry) {
}

func registerPreviewDNSTransports(registry *dns.TransportRegistry) {
}

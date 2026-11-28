//go:build !core_preview

package libcore

import (
	"net"

	"github.com/sagernet/sing-box/adapter"
	"github.com/sagernet/sing-box/option"
)

func connectionOwner(uid int32, name string) *adapter.ConnectionOwner {
	return &adapter.ConnectionOwner{UserId: uid, AndroidPackageNames: []string{name}}
}

func (p *platformLocalDNSTransport) Start(stage adapter.StartStage) error {
	return nil
}

func setGeoIPRuleCIDRs(rule *option.DefaultHeadlessRule, ipNets []*net.IPNet) {
	rule.IPCIDR = make([]string, 0, len(ipNets))
	for _, cidr := range ipNets {
		rule.IPCIDR = append(rule.IPCIDR, cidr.String())
	}
}

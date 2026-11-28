//go:build core_preview

package libcore

import (
	"net"
	"net/netip"
	"os"

	"github.com/sagernet/sing-box/adapter"
	"github.com/sagernet/sing-box/option"
	"github.com/sagernet/sing/common/json/badoption"
)

func connectionOwner(uid int32, name string) *adapter.ConnectionOwner {
	return &adapter.ConnectionOwner{UserId: uid, PackageNames: []string{name}}
}

func (s *platformStub) UsePlatformAutoRedirect() bool { return false }

func (s *platformStub) CreateAutoRedirect(options adapter.AutoRedirectOptions) (adapter.AutoRedirectSession, error) {
	return nil, os.ErrInvalid
}

func (p *platformLocalDNSTransport) Start(stage adapter.StartStage, scope *adapter.Scope) error {
	return nil
}

func setGeoIPRuleCIDRs(rule *option.DefaultHeadlessRule, ipNets []*net.IPNet) {
	rule.IPCIDR = make(badoption.Listable[*badoption.Prefixable], 0, len(ipNets))
	for _, cidr := range ipNets {
		if prefix, err := netip.ParsePrefix(cidr.String()); err == nil {
			p := badoption.Prefixable(prefix)
			rule.IPCIDR = append(rule.IPCIDR, &p)
		}
	}
}

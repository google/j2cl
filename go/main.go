package main

import (
	"fmt"
	"log"
	"net"
	"os"
	"sort"
	"strings"
)

type longestToShortest []string

func (s longestToShortest) Len() int {
	return len(s)
}

func (s longestToShortest) Swap(i, j int) {
	s[i], s[j] = s[j], s[i]
}

func (s longestToShortest) Less(i, j int) bool {
	return len(s[i]) > len(s[j])
}

func FQDN() (string, error) {
	hostname, err := os.Hostname()
	if err != nil {
		// Fail if the kernel fails to report a hostname.
		return "", err
	}

	addrs, err := net.LookupHost(hostname)
	if err != nil {
    fmt.Printf("net.LookupHost failed for %s (error: %v)\n", hostname, err)
		return "localhost", nil
	}

  fmt.Printf("Addresses %s\n", addrs)
	for _, addr := range addrs {
    fmt.Printf("Looking up %s\n", addr)
		if names, err := net.LookupAddr(addr); err == nil && len(names) > 0 {
      fmt.Printf("Names %s\n", names)
			sort.Sort(longestToShortest(names))
			for _, name := range names {
				name = strings.TrimRight(name, ".")
				if strings.HasPrefix(name, hostname) {
					return name, nil
				}
			}
			return names[0], nil
		}
	}

  fmt.Printf("Host name from address failed")
	return "localhost", nil
}

func main() {
	fqdn, err := FQDN()
	if err != nil {
		log.Fatalf("could not get FQDN: %v", err)
	}
	fmt.Printf("Resolved FQDN: %s\n", fqdn)
}

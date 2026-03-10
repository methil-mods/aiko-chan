package handlers

import (
	"log"
	"net/http"
	"net/http/httputil"
	"net/url"
	"os"
)

// ProxyHandler gère la redirection des requêtes vers le backend Modal
type ProxyHandler struct {
	Proxy *httputil.ReverseProxy
	Target *url.URL
}

// NewProxyHandler initialise un nouveau handler de proxy
func NewProxyHandler() (*ProxyHandler, error) {
	modalURLStr := os.Getenv("MODAL_URL")
	if modalURLStr == "" {
		modalURLStr = "https://your-modal-app-url.modal.run"
	}

	target, err := url.Parse(modalURLStr)
	if err != nil {
		return nil, err
	}

	apiKey := os.Getenv("AIKO_API_KEY")

	proxy := httputil.NewSingleHostReverseProxy(target)
	
	originalDirector := proxy.Director
	proxy.Director = func(req *http.Request) {
		originalDirector(req)
		req.Host = target.Host
		if apiKey != "" {
			req.Header.Set("Authorization", "Bearer "+apiKey)
		}
		log.Printf("[Proxy] %s %s -> %s", req.Method, req.URL.Path, target.Host)
	}

	return &ProxyHandler{
		Proxy:  proxy,
		Target: target,
	}, nil
}

func (h *ProxyHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	h.Proxy.ServeHTTP(w, r)
}

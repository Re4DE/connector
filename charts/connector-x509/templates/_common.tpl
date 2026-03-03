{{/*
Copyright (c) 2025 Fraunhofer Institute for Energy Economics and Energy System Technology (IEE)

This program and the accompanying materials are made available under the
terms of the Apache License, Version 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0

SPDX-License-Identifier: Apache-2.0

Contributors:
     Fraunhofer IEE - initial API and implementation
*/}}

{{/*
Allow the release namespace to be overridden for multi-namespace deployments in combined charts
*/}}
{{- define "common.namespace" -}}
{{- default .Release.Namespace .Values.namespaceOverride | trimSuffix "-" }}
{{- end -}}

{{/*
Extract the url of the enabled vault instance
*/}}
{{- define "common.vault.url" }}
{{- printf "http://%s-vault:8200" .Release.Name | quote }}
{{- end -}}

{{/*
Generates the controlplane labels
*/}}
{{- define "controlplane.labels" -}}
app.kubernetes.io/name: connector
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/component: controlplane
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{- end -}}

{{/*
Generates controlplane labels to match immutable field like deployment templates or services
*/}}
{{- define "controlplane.matchLabels" -}}
app.kubernetes.io/name: connector
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/component: controlplane
{{- end -}}

{{/*
Uses the overrite audience or the default one which is also the token url
*/}}
{{- define "controlplane.auth.oauth.provider_audience" -}}
{{- default .Values.controlplane.edc.auth.oauth.tokenUrl .Values.controlplane.edc.auth.oauth.providerAudienceOverride }}
{{- end -}}

{{/*
Generates the datalplane labels
*/}}
{{- define "dataplane.labels" -}}
app.kubernetes.io/name: connector
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/component: dataplane
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{- end -}}

{{/*
Generates dataplane labels to match immutable field like deployment templates or services
*/}}
{{- define "dataplane.matchLabels" -}}
app.kubernetes.io/name: connector
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/component: dataplane
{{- end -}}

{{/*
Generates the data plane control url
*/}}
{{- define "dataplane.controlUrl" -}}
{{- printf "http://dataplane:%v%s" .Values.controlplane.endpoints.control.port .Values.controlplane.endpoints.control.path | quote }}
{{- end -}}

{{/*
Generates the data plane selector url
*/}}
{{- define "dataplane.dfpUrl" -}}
{{- printf "http://controlplane:%v%s/v1/dataplanes" .Values.controlplane.endpoints.control.port .Values.controlplane.endpoints.control.path | quote }}
{{- end -}}

{{/*
Generates the base public api url
*/}}
{{- define "dataplane.publicUrl" -}}
{{- printf "https://%s%s" .Values.dataplane.ingress.host .Values.dataplane.endpoints.public.path | quote }}
{{- end -}}

{{/*
Generates the ui labels
*/}}
{{- define "ui.labels" -}}
app.kubernetes.io/name: connector
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/component: ui
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{- end -}}

{{/*
Generates ui labels to match immutable field like deployment templates or services
*/}}
{{- define "ui.matchLabels" -}}
app.kubernetes.io/name: connector
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/component: ui
{{- end -}}

{{/*
Generates the management api path of the controlplane for the ui
*/}}
{{- define "ui.managementUrl" -}}
{{- printf "https://%s%s/v3" .Values.controlplane.ingress.host .Values.controlplane.endpoints.management.path | quote }}
{{- end -}}

{{/*
Generates the default api path of the controlplane for the ui
*/}}
{{- define "ui.defaultUrl" -}}
{{- printf "https://%s%s" .Values.controlplane.ingress.host .Values.controlplane.endpoints.default.path | quote }}
{{- end -}}

{{/*
Generates the dsp api path of the controlplane for the ui
*/}}
{{- define "ui.dspUrl" -}}
{{- printf "https://%s%s" .Values.controlplane.ingress.host .Values.controlplane.endpoints.protocol.path | quote }}
{{- end -}}

{{/*
Generates the catalog api path of the controlplane for the ui
*/}}
{{- define "ui.catalogUrl" -}}
{{- printf "https://%s%s" .Values.controlplane.ingress.host .Values.controlplane.endpoints.catalog.path | quote }}
{{- end -}}
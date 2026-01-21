declare module 'bpmn-js-properties-panel' {
  export const BpmnPropertiesPanelModule: any
  export const BpmnPropertiesProviderModule: any
  export const CamundaPlatformPropertiesProviderModule: any
  export const ZeebePropertiesProviderModule: any
}

declare module '@bpmn-io/properties-panel' {
  export function TextFieldEntry(props: any): any
  export function SelectEntry(props: any): any
  export function TextAreaEntry(props: any): any
  export function CheckboxEntry(props: any): any
  export function isTextFieldEntryEdited(element: any, property: string): boolean
  export function isSelectEntryEdited(element: any, property: string): boolean
}

declare module 'bpmn-moddle' {
  export default class BpmnModdle {
    constructor(packages?: any)
    create(type: string, attrs?: any): any
    toXML(definitions: any, options?: any): Promise<{ xml: string }>
    fromXML(xml: string, options?: any): Promise<{ rootElement: any; warnings: any[] }>
  }
}

declare module 'camunda-bpmn-moddle/resources/camunda' {
  const CamundaBpmnModdle: any
  export default CamundaBpmnModdle
}

declare module 'camunda-bpmn-moddle/resources/camunda.json' {
  const CamundaBpmnModdle: any
  export default CamundaBpmnModdle
}

declare module 'camunda-bpmn-js-behaviors/lib/camunda-platform' {
  const CamundaPlatformBehaviorsModule: any
  export default CamundaPlatformBehaviorsModule
}

declare module 'camunda-bpmn-js-behaviors/lib/camunda-cloud' {
  const CamundaCloudBehaviorsModule: any
  export default CamundaCloudBehaviorsModule
}

declare module 'bpmn-auto-layout' {
  export function layoutProcess(xml: string): Promise<string>
}

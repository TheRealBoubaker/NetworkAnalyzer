
```ts
import mermaid from 'mermaid';

// Initialize mermaid with your preferred theme
mermaid.initialize({ startOnLoad: false, theme: 'forest' });

const drawDiagram = async () => {
  const element = document.querySelector('#diagram-container');
  const graphDefinition = `
    sequenceDiagram
      Alice->>Bob: Hello Bob, how are you?
      Bob-->>Alice: Jolly good!
  `;

  // Render returns the SVG code as a string
  const { svg } = await mermaid.render('id-for-diagram', graphDefinition);
  element.innerHTML = svg;
};

drawDiagram();

```



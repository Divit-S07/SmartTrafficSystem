import type { CSSProperties } from 'react';
import { Handle, Position } from 'reactflow';
import type { NodeProps } from 'reactflow';

export type JunctionNodeData = {
  junctionId: string;
  roadCount: number;
  isSelected: boolean;
  isConnected: boolean;
  hasVehicles: boolean;
};

export const JUNCTION_SIZE = 30;

const hiddenHandle: CSSProperties = {
  left: '50%',
  top: '50%',
  transform: 'translate(-50%, -50%)',
  background: 'transparent',
  border: 'none',
  width: 4,
  height: 4,
  minWidth: 4,
  minHeight: 4,
  opacity: 0,
  pointerEvents: 'none',
};

function labelFontSize(id: string): number {
  if (id.length <= 2) return 11;
  if (id.length === 3) return 9.5;
  if (id.length === 4) return 8.5;
  return 7.5;
}

export function JunctionNode({ data }: NodeProps<JunctionNodeData>) {
  const { junctionId, roadCount, isSelected, isConnected, hasVehicles } = data;

  const ring = isSelected ? '#06b6d4' : isConnected ? '#0284c7' : '#3b82f6';
  const ringWidth = isSelected ? 2 : 1.5;
  const boxShadow = isSelected
    ? '0 0 0 3px rgba(6, 182, 212, 0.25), 0 2px 6px rgba(15, 23, 42, 0.3)'
    : isConnected
      ? '0 0 0 2px rgba(2, 132, 199, 0.18), 0 2px 6px rgba(15, 23, 42, 0.28)'
      : '0 2px 6px rgba(15, 23, 42, 0.28)';

  return (
    <div
      style={{
        width: JUNCTION_SIZE,
        height: JUNCTION_SIZE,
        borderRadius: '50%',
        boxSizing: 'border-box',
        background: '#ffffff',
        border: `${ringWidth}px solid ${ring}`,
        boxShadow,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        position: 'relative',
        cursor: 'grab',
        userSelect: 'none',
      }}
      title={`Junction ${junctionId} \u00b7 ${roadCount} connected road${roadCount === 1 ? '' : 's'}`}
    >
      {hasVehicles && (
        <span
          aria-hidden
          className="pulse-cyan"
          style={{
            position: 'absolute',
            inset: -4,
            borderRadius: '50%',
            border: '1.5px solid #22d3ee',
            pointerEvents: 'none',
          }}
        />
      )}
      <span
        style={{
          fontFamily:
            'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace',
          fontWeight: 700,
          fontSize: labelFontSize(junctionId),
          color: '#0f172a',
          lineHeight: 1,
          whiteSpace: 'nowrap',
          pointerEvents: 'none',
        }}
      >
        {junctionId}
      </span>
      <Handle type="source" position={Position.Top} style={hiddenHandle} isConnectable={false} />
      <Handle type="target" position={Position.Top} style={hiddenHandle} isConnectable={false} />
    </div>
  );
}

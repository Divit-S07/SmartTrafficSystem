import { useCallback, useEffect, useMemo, useRef } from 'react';
import ReactFlow, { Background, BackgroundVariant, ReactFlowProvider, useReactFlow } from 'reactflow';
import type { Edge, Node, OnNodesChange, EdgeMouseHandler, NodeMouseHandler } from 'reactflow';
import 'reactflow/dist/style.css';
import { MapPin } from 'lucide-react';
import type { EmergencyVehicle, Junction } from '@/types';
import { EmptyState } from '@/components/ui/Common';
import { JunctionNode } from './JunctionNode';
import type { JunctionNodeData } from './JunctionNode';
import { RoadEdge } from './RoadEdge';
import { MapLegend } from './MapLegend';
import { MapToolbar } from './MapToolbar';
import { pairKey } from './networkGeometry';
import type { Selection, VisualRoad } from './networkGeometry';

type AppNode = Node<JunctionNodeData>;
type AppEdge = Edge<VisualRoad>;

const nodeTypes = { junction: JunctionNode };
const edgeTypes = { road: RoadEdge };
const SNAP_GRID: [number, number] = [20, 20];

interface NetworkMapProps {
  junctions: Junction[];
  vehicles: EmergencyVehicle[];
  visualRoads: VisualRoad[];
  selection: Selection;
  showGrid: boolean;
  onToggleGrid: () => void;
  onJunctionMoved: (id: string, x: number, y: number) => void;
  /** Fired once when a drag ends — the hook persists the new position. */
  onJunctionDragEnd: (id: string, x: number, y: number) => void;
  onJunctionClick: (id: string) => void;
  onRoadClick: (from: string, to: string) => void;
  onClearSelection: () => void;
  onAddJunction: () => void;
}

function NetworkCanvas({
  junctions,
  vehicles,
  visualRoads,
  selection,
  showGrid,
  onToggleGrid,
  onJunctionMoved,
  onJunctionDragEnd,
  onJunctionClick,
  onRoadClick,
  onClearSelection,
  onAddJunction,
}: NetworkMapProps) {
  const { fitView, setViewport } = useReactFlow();

  const roadCounts = useMemo(() => {
    const counts = new Map<string, number>();
    for (const road of visualRoads) {
      counts.set(road.from, (counts.get(road.from) ?? 0) + 1);
      counts.set(road.to, (counts.get(road.to) ?? 0) + 1);
    }
    return counts;
  }, [visualRoads]);

  const nodes = useMemo<AppNode[]>(
    () =>
      junctions.map((j) => ({
        id: j.id,
        type: 'junction',
        position: { x: j.x, y: j.y },
        deletable: false,
        data: {
          junctionId: j.id,
          roadCount: roadCounts.get(j.id) ?? 0,
          isSelected: selection?.kind === 'junction' && selection.id === j.id,
          isConnected:
            selection?.kind === 'road' && (selection.from === j.id || selection.to === j.id),
          hasVehicles: vehicles.some((v) => v.lastConfirmedJunction === j.id),
        },
      })),
    [junctions, roadCounts, selection, vehicles],
  );

  const edges = useMemo<AppEdge[]>(
    () =>
      visualRoads.map((road) => {
        const selected =
          selection?.kind === 'road'
            ? pairKey(selection.from, selection.to) === pairKey(road.from, road.to)
            : selection?.kind === 'junction' &&
              (selection.id === road.from || selection.id === road.to);
        return {
          id: road.edgeId,
          type: 'road',
          source: road.from,
          target: road.to,
          data: road,
          selected,
          deletable: false,
        };
      }),
    [visualRoads, selection],
  );

  const handleNodesChange = useCallback<OnNodesChange>(
    (changes) => {
      for (const change of changes) {
        if (change.type === 'position' && change.position) {
          // Move locally on every drag frame so the canvas stays responsive,
          // but only persist once the drag has actually finished.
          onJunctionMoved(change.id, change.position.x, change.position.y);
          if (change.dragging === false) {
            onJunctionDragEnd(change.id, change.position.x, change.position.y);
          }
        }
      }
    },
    [onJunctionMoved, onJunctionDragEnd],
  );

  const handleNodeClick = useCallback<NodeMouseHandler>(
    (_event, node) => onJunctionClick(node.id),
    [onJunctionClick],
  );

  const handleEdgeClick = useCallback<EdgeMouseHandler>(
    (_event, edge) => {
      const road = edge.data as VisualRoad | undefined;
      if (road) onRoadClick(road.from, road.to);
    },
    [onRoadClick],
  );

  const handleResetView = useCallback(() => {
    if (junctions.length > 0) {
      fitView({ padding: 0.2, maxZoom: 1, duration: 350 });
    } else {
      setViewport({ x: 0, y: 0, zoom: 1 }, { duration: 350 });
    }
  }, [junctions.length, fitView, setViewport]);

  const prevCountRef = useRef(junctions.length);
  useEffect(() => {
    const prev = prevCountRef.current;
    prevCountRef.current = junctions.length;
    if (prev === 0 && junctions.length > 0) {
      const timer = setTimeout(() => {
        fitView({ padding: 0.3, duration: 400, maxZoom: 1.4 });
      }, 80);
      return () => clearTimeout(timer);
    }
    return undefined;
  }, [junctions.length, fitView]);

  return (
    <div className="relative h-[480px] overflow-hidden rounded-xl border border-slate-800 bg-[#eef2f7] shadow-md sm:h-[540px] lg:h-[600px]">
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={handleNodesChange}
        onNodeClick={handleNodeClick}
        onEdgeClick={handleEdgeClick}
        onPaneClick={onClearSelection}
        nodeTypes={nodeTypes}
        edgeTypes={edgeTypes}
        nodeOrigin={[0.5, 0.5]}
        nodesConnectable={false}
        elementsSelectable={false}
        deleteKeyCode={null}
        minZoom={0.25}
        maxZoom={2.5}
        snapToGrid={showGrid}
        snapGrid={SNAP_GRID}
      >
        {showGrid && (
          <Background variant={BackgroundVariant.Lines} gap={24} lineWidth={1} color="#ccd6e2" />
        )}
      </ReactFlow>

      <MapToolbar showGrid={showGrid} onToggleGrid={onToggleGrid} onResetView={handleResetView} />

      {junctions.length > 0 && <MapLegend />}

      {junctions.length === 0 && (
        <div className="pointer-events-none absolute inset-0 z-10 flex items-center justify-center p-4">
          <div className="pointer-events-auto card max-w-sm p-2">
            <EmptyState
              icon={<MapPin className="h-8 w-8" />}
              title="Your road network is empty."
              message="Add a junction to get started."
              actionLabel="Add Junction"
              onAction={onAddJunction}
            />
          </div>
        </div>
      )}
    </div>
  );
}

export function NetworkMap(props: NetworkMapProps) {
  return (
    <ReactFlowProvider>
      <NetworkCanvas {...props} />
    </ReactFlowProvider>
  );
}
